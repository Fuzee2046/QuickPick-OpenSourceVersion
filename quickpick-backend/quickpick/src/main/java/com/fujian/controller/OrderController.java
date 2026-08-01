package com.fujian.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fujian.common.Result;
import com.fujian.mapper.DishMapper;
import com.fujian.mapper.DishOptionGroupBindingMapper;
import com.fujian.mapper.DishOptionGroupMapper;
import com.fujian.mapper.DishOptionValueMapper;
import com.fujian.mapper.OrderWeightItemMapper;
import com.fujian.mapper.OrderItemMapper;
import com.fujian.mapper.OrderItemOptionMapper;
import com.fujian.mapper.OrderMapper;
import com.fujian.mapper.ShopBrothOptionMapper;
import com.fujian.mapper.ShopMapper;
import com.fujian.mapper.UserMapper;
import com.fujian.mapper.WeightIngredientMapper;
import com.fujian.pojo.Dish;
import com.fujian.pojo.DishOptionGroup;
import com.fujian.pojo.DishOptionGroupBinding;
import com.fujian.pojo.DishOptionValue;
import com.fujian.pojo.Order;
import com.fujian.pojo.OrderItem;
import com.fujian.pojo.OrderItemOption;
import com.fujian.pojo.OrderWeightItem;
import com.fujian.pojo.Shop;
import com.fujian.pojo.ShopBrothOption;
import com.fujian.pojo.User;
import com.fujian.pojo.WeightIngredient;
import com.fujian.service.OrderIdGeneratorService;
import com.fujian.service.OrderSubmissionIdempotencyService;
import com.fujian.service.ReservationRuleService;
import com.fujian.service.WechatSubscribeMessageService;
import com.fujian.service.MerchantBillingService;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class OrderController {
    private static final List<String> USER_NO_SHOW_CANCEL_REASONS = Arrays.asList("用户超时未取", "用户没来取餐");
    private static final int PICKUP_OVERTIME_REMINDER_MINUTES = 15;
    private static final int MAX_ACTIVE_ORDER_COUNT = 2;
    private static final int TIME_SLOT_STEP_MINUTES = 5;
    private static final LocalTime RESERVATION_OPEN_TIME = LocalTime.of(7, 30);
    private static final String PENALTY_STATUS_NORMAL = "normal";
    private static final String RESTRICTION_LEVEL_ACTIVE_ORDER_LIMIT = "active_order_limit";
    private static final String PENALTY_STATUS_BLOCKED_3D = "blocked_3d";
    private static final String PENALTY_STATUS_BLOCKED_7D = "blocked_7d";
    private static final String PENALTY_STATUS_BLOCKED_30D = "blocked_30d";
    private static final String PENALTY_STATUS_FROZEN = "frozen";
    private static final String DEFAULT_FROZEN_CONTACT_NOTE = "如果这次并非故意为之，可以联系我处理恢复，也希望你能理解并支持。";
    private static final String ORDER_MODE_FIXED_DISH = "fixed_dish";
    private static final String ORDER_MODE_WEIGHT_SELECTION = "weight_selection";
    private static final String PRICING_STATUS_NOT_REQUIRED = "not_required";
    private static final String PRICING_STATUS_PENDING_CONFIRM = "pending_confirm";
    private static final String PRICING_STATUS_CONFIRMED = "confirmed";
    private static final DateTimeFormatter PENALTY_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderItemMapper orderItemMapper;

    @Autowired
    private OrderWeightItemMapper orderWeightItemMapper;

    @Autowired
    private OrderItemOptionMapper orderItemOptionMapper;

    @Autowired
    private ShopMapper shopMapper;

    @Autowired
    private DishMapper dishMapper;

    @Autowired
    private DishOptionGroupMapper dishOptionGroupMapper;

    @Autowired
    private DishOptionValueMapper dishOptionValueMapper;

    @Autowired
    private DishOptionGroupBindingMapper dishOptionGroupBindingMapper;

    @Autowired
    private WeightIngredientMapper weightIngredientMapper;

    @Autowired
    private ShopBrothOptionMapper shopBrothOptionMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private OrderIdGeneratorService orderIdGeneratorService;

    @Autowired
    private WechatSubscribeMessageService wechatSubscribeMessageService;

    @Autowired
    private ReservationRuleService reservationRuleService;

    @Autowired
    private MerchantBillingService merchantBillingService;

    @Autowired
    private OrderSubmissionIdempotencyService orderSubmissionIdempotencyService;

    private boolean hasRole(Authentication authentication, String role) {
        return authentication != null
                && authentication.getAuthorities() != null
                && authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role::equals);
    }

    private Long getCurrentUserId() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (!hasRole(authentication, "ROLE_USER")) {
                return null;
            }
            return (Long) authentication.getPrincipal();
        } catch (Exception e) {
            return null;
        }
    }

    private Long getCurrentMerchantShopId() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (!hasRole(authentication, "ROLE_MERCHANT")) {
                return null;
            }
            return (Long) authentication.getPrincipal();
        } catch (Exception e) {
            return null;
        }
    }

    public static class OrderRequest {
        private Long shopId;
        // private String openid; // Deprecated, use token
        private String clientRequestId;
        private LocalTime pickupTime;
        private Integer needPack; // 0 or 1
        private String remark;
        private List<OrderItemRequest> items;
        private Long brothOptionId;
        private List<WeightItemRequest> weightItems;

        public Long getShopId() { return shopId; }
        public void setShopId(Long shopId) { this.shopId = shopId; }
        public String getClientRequestId() { return clientRequestId; }
        public void setClientRequestId(String clientRequestId) { this.clientRequestId = clientRequestId; }
        public LocalTime getPickupTime() { return pickupTime; }
        public void setPickupTime(LocalTime pickupTime) { this.pickupTime = pickupTime; }
        public Integer getNeedPack() { return needPack; }
        public void setNeedPack(Integer needPack) { this.needPack = needPack; }
        public String getRemark() { return remark; }
        public void setRemark(String remark) { this.remark = remark; }
        public List<OrderItemRequest> getItems() { return items; }
        public void setItems(List<OrderItemRequest> items) { this.items = items; }
        public Long getBrothOptionId() { return brothOptionId; }
        public void setBrothOptionId(Long brothOptionId) { this.brothOptionId = brothOptionId; }
        public List<WeightItemRequest> getWeightItems() { return weightItems; }
        public void setWeightItems(List<WeightItemRequest> weightItems) { this.weightItems = weightItems; }
    }

    public static class OrderItemRequest {
        private Long dishId;
        private Integer quantity;
        private List<OrderItemOptionRequest> selectedOptions;

        public Long getDishId() { return dishId; }
        public void setDishId(Long dishId) { this.dishId = dishId; }
        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }
        public List<OrderItemOptionRequest> getSelectedOptions() { return selectedOptions; }
        public void setSelectedOptions(List<OrderItemOptionRequest> selectedOptions) { this.selectedOptions = selectedOptions; }
    }

    public static class OrderItemOptionRequest {
        private Long optionGroupId;
        private Long optionValueId;

        public Long getOptionGroupId() { return optionGroupId; }
        public void setOptionGroupId(Long optionGroupId) { this.optionGroupId = optionGroupId; }
        public Long getOptionValueId() { return optionValueId; }
        public void setOptionValueId(Long optionValueId) { this.optionValueId = optionValueId; }
    }

    public static class WeightItemRequest {
        private Long ingredientId;
        private Integer quantity;

        public Long getIngredientId() { return ingredientId; }
        public void setIngredientId(Long ingredientId) { this.ingredientId = ingredientId; }
        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }
    }

    public static class ConfirmWeightOrderRequest {
        private Integer finalWeightG;
        private BigDecimal finalAmount;
        private String priceEvidenceImage;

        public Integer getFinalWeightG() { return finalWeightG; }
        public void setFinalWeightG(Integer finalWeightG) { this.finalWeightG = finalWeightG; }
        public BigDecimal getFinalAmount() { return finalAmount; }
        public void setFinalAmount(BigDecimal finalAmount) { this.finalAmount = finalAmount; }
        public String getPriceEvidenceImage() { return priceEvidenceImage; }
        public void setPriceEvidenceImage(String priceEvidenceImage) { this.priceEvidenceImage = priceEvidenceImage; }
    }

    public static class ResolveStaleOrderRequest {
        private String result;
        private String cancelReason;

        public String getResult() { return result; }
        public void setResult(String result) { this.result = result; }
        public String getCancelReason() { return cancelReason; }
        public void setCancelReason(String cancelReason) { this.cancelReason = cancelReason; }
    }

    @Data
    public static class OrderRiskStatus {
        private int noShowCount;
        private int currentActiveOrderCount;
        private String penaltyStatus;
        private String restrictionLevel;
        private String restrictionMessage;
        private String penaltyReason;
        private LocalDateTime penaltyEndTime;
        private String frozenContactNote;

        public int getNoShowCount() { return noShowCount; }
        public void setNoShowCount(int noShowCount) { this.noShowCount = noShowCount; }
        public int getCurrentActiveOrderCount() { return currentActiveOrderCount; }
        public void setCurrentActiveOrderCount(int currentActiveOrderCount) { this.currentActiveOrderCount = currentActiveOrderCount; }
        public String getPenaltyStatus() { return penaltyStatus; }
        public void setPenaltyStatus(String penaltyStatus) { this.penaltyStatus = penaltyStatus; }
        public String getRestrictionLevel() { return restrictionLevel; }
        public void setRestrictionLevel(String restrictionLevel) { this.restrictionLevel = restrictionLevel; }
        public String getRestrictionMessage() { return restrictionMessage; }
        public void setRestrictionMessage(String restrictionMessage) { this.restrictionMessage = restrictionMessage; }
        public String getPenaltyReason() { return penaltyReason; }
        public void setPenaltyReason(String penaltyReason) { this.penaltyReason = penaltyReason; }
        public LocalDateTime getPenaltyEndTime() { return penaltyEndTime; }
        public void setPenaltyEndTime(LocalDateTime penaltyEndTime) { this.penaltyEndTime = penaltyEndTime; }
        public String getFrozenContactNote() { return frozenContactNote; }
        public void setFrozenContactNote(String frozenContactNote) { this.frozenContactNote = frozenContactNote; }
    }

    @Data
    public static class DailyIntegrityStats {
        private int integrityCount;
        private int noShowCount;
        private int totalCount;
        private int integrityRate;
        private int noShowRate;
        private String highlightText;

        public int getIntegrityCount() { return integrityCount; }
        public void setIntegrityCount(int integrityCount) { this.integrityCount = integrityCount; }
        public int getNoShowCount() { return noShowCount; }
        public void setNoShowCount(int noShowCount) { this.noShowCount = noShowCount; }
        public int getTotalCount() { return totalCount; }
        public void setTotalCount(int totalCount) { this.totalCount = totalCount; }
        public int getIntegrityRate() { return integrityRate; }
        public void setIntegrityRate(int integrityRate) { this.integrityRate = integrityRate; }
        public int getNoShowRate() { return noShowRate; }
        public void setNoShowRate(int noShowRate) { this.noShowRate = noShowRate; }
        public String getHighlightText() { return highlightText; }
        public void setHighlightText(String highlightText) { this.highlightText = highlightText; }
    }

    private DailyIntegrityStats buildDailyIntegrityStats() {
        LocalDate today = LocalDate.now();

        Long integrityCountValue = orderMapper.selectCount(new LambdaQueryWrapper<Order>()
                .eq(Order::getBizDate, today)
                .in(Order::getStatus, Arrays.asList("pending", "making", "completed")));

        Long noShowCountValue = orderMapper.selectCount(new LambdaQueryWrapper<Order>()
                .eq(Order::getBizDate, today)
                .eq(Order::getStatus, "cancelled")
                .in(Order::getCancelReason, USER_NO_SHOW_CANCEL_REASONS));

        int integrityCount = integrityCountValue == null ? 0 : integrityCountValue.intValue();
        int noShowCount = noShowCountValue == null ? 0 : noShowCountValue.intValue();
        int totalCount = integrityCount + noShowCount;
        int integrityRate = totalCount == 0 ? 100 : (int) Math.round(integrityCount * 100.0 / totalCount);
        int noShowRate = totalCount == 0 ? 0 : Math.max(0, 100 - integrityRate);

        DailyIntegrityStats stats = new DailyIntegrityStats();
        stats.setIntegrityCount(integrityCount);
        stats.setNoShowCount(noShowCount);
        stats.setTotalCount(totalCount);
        stats.setIntegrityRate(integrityRate);
        stats.setNoShowRate(noShowRate);

        if (noShowCount == 0) {
            stats.setHighlightText("今日诚信完胜，继续保持");
        } else if (integrityRate >= 90) {
            stats.setHighlightText("大部分同学都在按时取餐");
        } else {
            stats.setHighlightText("按时取餐，能让商户备餐更安心");
        }

        return stats;
    }

    private boolean isTasteSensitiveShop(Shop shop) {
        return shop != null && shop.getTasteSensitiveEnabled() != null && shop.getTasteSensitiveEnabled() == 1;
    }

    private LocalDateTime buildPickupBaseTime(Order order) {
        if (order == null || order.getBizDate() == null || order.getPickupTime() == null) {
            return null;
        }

        LocalDateTime scheduledPickupTime = LocalDateTime.of(order.getBizDate(), order.getPickupTime());
        if (order.getReadyTime() == null || !order.getReadyTime().isAfter(scheduledPickupTime)) {
            return scheduledPickupTime;
        }

        return order.getReadyTime();
    }

    private int calculatePickupOvertimeMinutes(Order order, LocalDateTime actualPickupTime) {
        LocalDateTime baseTime = buildPickupBaseTime(order);
        if (baseTime == null || actualPickupTime == null || !actualPickupTime.isAfter(baseTime)) {
            return 0;
        }

        return (int) Duration.between(baseTime, actualPickupTime).toMinutes();
    }

    private String buildPickupOvertimeNote(Order order, int overtimeMinutes, LocalDateTime actualPickupTime) {
        if (order == null || overtimeMinutes <= 0 || actualPickupTime == null) {
            return null;
        }

        String pickupTimeText = order.getPickupTime() != null ? order.getPickupTime().toString().substring(0, 5) : "未知";
        String readyTimeText = order.getReadyTime() != null ? order.getReadyTime().toLocalTime().toString().substring(0, 5) : "未知";
        String completedTimeText = actualPickupTime.toLocalTime().toString().substring(0, 5);

        return String.format(
                "预计%s，出餐%s，完成取餐%s，用户超时取餐%d分钟",
                pickupTimeText,
                readyTimeText,
                completedTimeText,
                overtimeMinutes
        );
    }

    private boolean isUserNoShowCancelReason(String cancelReason) {
        return cancelReason != null && USER_NO_SHOW_CANCEL_REASONS.contains(cancelReason.trim());
    }

    private boolean isTimedPenaltyStatus(String penaltyStatus) {
        return PENALTY_STATUS_BLOCKED_3D.equals(penaltyStatus)
                || PENALTY_STATUS_BLOCKED_7D.equals(penaltyStatus)
                || PENALTY_STATUS_BLOCKED_30D.equals(penaltyStatus);
    }

    private String formatPenaltyEndTime(LocalDateTime penaltyEndTime) {
        if (penaltyEndTime == null) {
            return "";
        }
        return penaltyEndTime.format(PENALTY_TIME_FORMATTER);
    }

    private LocalTime roundUpToStep(LocalTime time, int stepMinutes) {
        if (time == null || stepMinutes <= 0) {
            return time;
        }
        int totalMinutes = time.getHour() * 60 + time.getMinute();
        int roundedMinutes = ((totalMinutes + stepMinutes - 1) / stepMinutes) * stepMinutes;
        if (roundedMinutes >= 24 * 60) {
            roundedMinutes = 24 * 60 - stepMinutes;
        }
        return LocalTime.of(roundedMinutes / 60, roundedMinutes % 60);
    }

    private String buildPenaltyContactSuffix(User user) {
        if (user == null) {
            return "";
        }
        String contactNote = user.getFrozenContactNote();
        if (contactNote == null || contactNote.trim().isEmpty()) {
            contactNote = DEFAULT_FROZEN_CONTACT_NOTE;
        }
        return "，" + contactNote;
    }

    private String normalizeOrderRemark(String remark) {
        if (remark == null) {
            return null;
        }
        String trimmedRemark = remark.trim();
        return trimmedRemark.isEmpty() ? null : trimmedRemark;
    }

    private int calculateWeightEvidenceTolerance(int estimatedWeightG) {
        if (estimatedWeightG <= 0) {
            return 0;
        }
        return Math.max(50, (int) Math.ceil(estimatedWeightG * 0.12));
    }

    private boolean isPriceEvidenceRequired(Integer estimatedWeightG, Integer finalWeightG) {
        int estimatedWeight = estimatedWeightG == null ? 0 : estimatedWeightG;
        int finalWeight = finalWeightG == null ? 0 : finalWeightG;
        if (estimatedWeight <= 0 || finalWeight <= 0) {
            return false;
        }
        return finalWeight > estimatedWeight + calculateWeightEvidenceTolerance(estimatedWeight);
    }

    private boolean isWeightSelectionShop(Shop shop) {
        return shop != null && ORDER_MODE_WEIGHT_SELECTION.equals(shop.getShopMode());
    }

    private String normalizeClientRequestId(String clientRequestId) {
        if (clientRequestId == null) {
            return null;
        }
        String normalized = clientRequestId.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private Order findExistingOrderByClientRequestId(Long userId, String clientRequestId) {
        if (userId == null || clientRequestId == null) {
            return null;
        }
        return orderMapper.selectOne(new LambdaQueryWrapper<Order>()
                .eq(Order::getUserId, userId)
                .eq(Order::getClientRequestId, clientRequestId)
                .last("LIMIT 1"));
    }

    private BigDecimal calculateWeightSelectionAmount(int totalWeightG, BigDecimal pricePer500g, BigDecimal brothExtraPrice, Integer needPack) {
        if (pricePer500g == null || totalWeightG <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal weightAmount = pricePer500g
                .multiply(new BigDecimal(totalWeightG))
                .divide(new BigDecimal("500"), 2, RoundingMode.HALF_UP);
        BigDecimal result = weightAmount.add(brothExtraPrice == null ? BigDecimal.ZERO : brothExtraPrice);
        if (needPack != null && needPack == 1) {
            result = result.add(new BigDecimal("1.00"));
        }
        return result;
    }

    private Map<Long, DishOptionGroupBinding> getDishBindingMap(Long dishId) {
        List<DishOptionGroupBinding> bindings = dishOptionGroupBindingMapper.selectList(new LambdaQueryWrapper<DishOptionGroupBinding>()
                .eq(DishOptionGroupBinding::getDishId, dishId)
                .orderByAsc(DishOptionGroupBinding::getSort)
                .orderByAsc(DishOptionGroupBinding::getId));
        Map<Long, DishOptionGroupBinding> result = new LinkedHashMap<>();
        for (DishOptionGroupBinding binding : bindings) {
            result.put(binding.getOptionGroupId(), binding);
        }
        return result;
    }

    private Map<Long, DishOptionGroup> getOptionGroupMap(Set<Long> groupIds) {
        if (groupIds == null || groupIds.isEmpty()) {
            return new LinkedHashMap<>();
        }
        return dishOptionGroupMapper.selectList(new LambdaQueryWrapper<DishOptionGroup>()
                        .in(DishOptionGroup::getId, groupIds))
                .stream()
                .collect(Collectors.toMap(DishOptionGroup::getId, item -> item));
    }

    private Map<Long, DishOptionValue> getOptionValueMap(Set<Long> groupIds) {
        if (groupIds == null || groupIds.isEmpty()) {
            return new LinkedHashMap<>();
        }
        return dishOptionValueMapper.selectList(new LambdaQueryWrapper<DishOptionValue>()
                        .in(DishOptionValue::getOptionGroupId, groupIds))
                .stream()
                .collect(Collectors.toMap(DishOptionValue::getId, item -> item));
    }

    private List<OrderItemOption> buildOrderItemOptions(
            Dish dish,
            OrderItemRequest itemReq,
            BigDecimal[] optionExtraTotalHolder,
            String[] errorHolder
    ) {
        List<OrderItemOption> result = new ArrayList<>();
        optionExtraTotalHolder[0] = BigDecimal.ZERO;

        Map<Long, DishOptionGroupBinding> bindingMap = getDishBindingMap(dish.getId());
        List<OrderItemOptionRequest> selectedOptions = itemReq.getSelectedOptions() == null
                ? new ArrayList<>()
                : itemReq.getSelectedOptions();

        if ((dish.getOptionEnabled() == null || dish.getOptionEnabled() != 1) && !selectedOptions.isEmpty()) {
            errorHolder[0] = "菜品 " + dish.getName() + " 当前不支持规格选择";
            return result;
        }

        if ((dish.getOptionEnabled() != null && dish.getOptionEnabled() == 1) && bindingMap.isEmpty()) {
            errorHolder[0] = "菜品 " + dish.getName() + " 的规格配置不完整，请联系商户";
            return result;
        }

        if ((dish.getOptionEnabled() == null || dish.getOptionEnabled() != 1) && bindingMap.isEmpty()) {
            return result;
        }

        Set<Long> groupIds = new HashSet<>(bindingMap.keySet());
        Map<Long, DishOptionGroup> groupMap = getOptionGroupMap(groupIds);
        Map<Long, DishOptionValue> valueMap = getOptionValueMap(groupIds);
        Set<Long> selectedGroupIds = new HashSet<>();

        for (OrderItemOptionRequest optionRequest : selectedOptions) {
            if (optionRequest == null || optionRequest.getOptionGroupId() == null || optionRequest.getOptionValueId() == null) {
                errorHolder[0] = "菜品 " + dish.getName() + " 的规格参数不完整";
                return result;
            }
            DishOptionGroupBinding binding = bindingMap.get(optionRequest.getOptionGroupId());
            if (binding == null) {
                errorHolder[0] = "菜品 " + dish.getName() + " 的规格选择无效";
                return result;
            }
            if (!selectedGroupIds.add(optionRequest.getOptionGroupId())) {
                errorHolder[0] = "菜品 " + dish.getName() + " 的同一规格组只能选择一个选项";
                return result;
            }

            DishOptionGroup group = groupMap.get(optionRequest.getOptionGroupId());
            DishOptionValue value = valueMap.get(optionRequest.getOptionValueId());
            if (group == null || value == null || !optionRequest.getOptionGroupId().equals(value.getOptionGroupId())) {
                errorHolder[0] = "菜品 " + dish.getName() + " 的规格值不存在";
                return result;
            }

            OrderItemOption orderItemOption = new OrderItemOption();
            orderItemOption.setOptionGroupId(group.getId());
            orderItemOption.setOptionValueId(value.getId());
            orderItemOption.setGroupName(group.getName());
            orderItemOption.setValueName(value.getName());
            orderItemOption.setExtraPrice(value.getExtraPrice() == null ? BigDecimal.ZERO : value.getExtraPrice());
            orderItemOption.setSort(binding.getSort() == null ? 0 : binding.getSort());
            result.add(orderItemOption);
            optionExtraTotalHolder[0] = optionExtraTotalHolder[0].add(orderItemOption.getExtraPrice());
        }

        for (DishOptionGroupBinding binding : bindingMap.values()) {
            boolean required = binding.getRequired() == null || binding.getRequired() == 1;
            if (required && !selectedGroupIds.contains(binding.getOptionGroupId())) {
                DishOptionGroup group = groupMap.get(binding.getOptionGroupId());
                errorHolder[0] = "请为菜品 " + dish.getName() + " 选择" + (group != null ? group.getName() : "规格");
                return new ArrayList<>();
            }
        }

        result.sort(Comparator.comparing(item -> item.getSort() == null ? 0 : item.getSort()));
        return result;
    }

    private Map<Long, List<OrderItemOption>> getOrderItemOptionMap(List<Long> orderItemIds) {
        Map<Long, List<OrderItemOption>> result = new LinkedHashMap<>();
        if (orderItemIds == null || orderItemIds.isEmpty()) {
            return result;
        }
        List<OrderItemOption> options = orderItemOptionMapper.selectList(new LambdaQueryWrapper<OrderItemOption>()
                .in(OrderItemOption::getOrderItemId, orderItemIds)
                .orderByAsc(OrderItemOption::getSort)
                .orderByAsc(OrderItemOption::getId));
        for (OrderItemOption option : options) {
            result.computeIfAbsent(option.getOrderItemId(), key -> new ArrayList<>()).add(option);
        }
        return result;
    }

    private String buildOptionSummary(List<OrderItemOption> options) {
        if (options == null || options.isEmpty()) {
            return null;
        }
        return options.stream()
                .map(OrderItemOption::getValueName)
                .collect(Collectors.joining(" / "));
    }

    private List<Map<String, Object>> buildOrderDisplayItems(Order order) {
        List<Map<String, Object>> itemDTOs = new ArrayList<>();
        if (ORDER_MODE_WEIGHT_SELECTION.equals(order.getOrderMode())) {
            List<OrderWeightItem> weightItems = orderWeightItemMapper.selectList(new LambdaQueryWrapper<OrderWeightItem>()
                    .eq(OrderWeightItem::getOrderId, order.getId()));
            for (OrderWeightItem weightItem : weightItems) {
                Map<String, Object> map = new java.util.HashMap<>();
                WeightIngredient ingredient = weightIngredientMapper.selectById(weightItem.getIngredientId());
                map.put("dishName", ingredient != null ? ingredient.getName() : "已删除食材");
                map.put("quantity", weightItem.getQuantity());
                map.put("price", null);
                map.put("image", ingredient != null ? ingredient.getImage() : null);
                itemDTOs.add(map);
            }
            return itemDTOs;
        }

        List<OrderItem> items = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderId, order.getId()));
        Map<Long, List<OrderItemOption>> optionMap = getOrderItemOptionMap(
                items.stream().map(OrderItem::getId).collect(Collectors.toList())
        );
        for (OrderItem item : items) {
            Map<String, Object> map = new java.util.HashMap<>();
            map.put("dishName", item.getDishName());
            map.put("quantity", item.getQuantity());
            map.put("price", item.getPrice());
            Dish dish = dishMapper.selectById(item.getDishId());
            if (dish != null) {
                map.put("image", dish.getImage());
            }
            List<OrderItemOption> options = optionMap.getOrDefault(item.getId(), new ArrayList<>());
            if (!options.isEmpty()) {
                List<Map<String, Object>> optionDTOs = new ArrayList<>();
                for (OrderItemOption option : options) {
                    Map<String, Object> optionDTO = new LinkedHashMap<>();
                    optionDTO.put("groupName", option.getGroupName());
                    optionDTO.put("valueName", option.getValueName());
                    optionDTO.put("extraPrice", option.getExtraPrice());
                    optionDTOs.add(optionDTO);
                }
                map.put("optionList", optionDTOs);
                map.put("options", buildOptionSummary(options));
            }
            itemDTOs.add(map);
        }
        return itemDTOs;
    }

    private void clearExpiredPenaltyIfNeeded(User user) {
        if (user == null || !isTimedPenaltyStatus(user.getPenaltyStatus()) || user.getPenaltyEndTime() == null) {
            return;
        }
        if (!LocalDateTime.now().isAfter(user.getPenaltyEndTime())) {
            return;
        }

        user.setPenaltyStatus(PENALTY_STATUS_NORMAL);
        user.setPenaltyEndTime(null);
        user.setPenaltyReason(null);
        user.setFrozenContactNote(null);
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);
    }

    private void applyNoShowPenalty(Order order, String cancelReason, LocalDateTime cancelTime) {
        if (order == null || order.getUserId() == null || !isUserNoShowCancelReason(cancelReason)) {
            return;
        }

        User user = userMapper.selectById(order.getUserId());
        if (user == null) {
            return;
        }

        clearExpiredPenaltyIfNeeded(user);

        int nextNoShowCount = user.getNoShowCount() == null ? 1 : user.getNoShowCount() + 1;
        user.setNoShowCount(nextNoShowCount);
        user.setLastNoShowOrderId(order.getId());

        if (nextNoShowCount >= 4) {
            user.setPenaltyStatus(PENALTY_STATUS_FROZEN);
            user.setPenaltyEndTime(null);
            user.setPenaltyReason("你已累计4次超时未取餐，预约权限已被冻结");
            if (user.getFrozenContactNote() == null || user.getFrozenContactNote().trim().isEmpty()) {
                user.setFrozenContactNote(DEFAULT_FROZEN_CONTACT_NOTE);
            }
        } else {
            int blockDays = nextNoShowCount == 1 ? 3 : (nextNoShowCount == 2 ? 7 : 30);
            String penaltyStatus = nextNoShowCount == 1
                    ? PENALTY_STATUS_BLOCKED_3D
                    : (nextNoShowCount == 2 ? PENALTY_STATUS_BLOCKED_7D : PENALTY_STATUS_BLOCKED_30D);
            LocalDateTime penaltyEndTime = cancelTime.plusDays(blockDays);

            user.setPenaltyStatus(penaltyStatus);
            user.setPenaltyEndTime(penaltyEndTime);
            user.setPenaltyReason(String.format("你已累计%d次超时未取餐，当前限制下单至%s", nextNoShowCount, formatPenaltyEndTime(penaltyEndTime)));
            user.setFrozenContactNote(DEFAULT_FROZEN_CONTACT_NOTE);
        }

        user.setUpdateTime(cancelTime);
        userMapper.updateById(user);
    }

    private int getCurrentActiveOrderCount(Long userId) {
        if (userId == null) {
            return 0;
        }

        Long count = orderMapper.selectCount(new LambdaQueryWrapper<Order>()
                .eq(Order::getUserId, userId)
                .in(Order::getStatus, Arrays.asList("making", "pending")));
        return count == null ? 0 : count.intValue();
    }

    private OrderRiskStatus buildOrderRiskStatus(Long userId) {
        OrderRiskStatus riskStatus = new OrderRiskStatus();
        riskStatus.setRestrictionLevel(PENALTY_STATUS_NORMAL);
        riskStatus.setPenaltyStatus(PENALTY_STATUS_NORMAL);

        if (userId == null) {
            return riskStatus;
        }

        User user = userMapper.selectById(userId);
        if (user == null) {
            return riskStatus;
        }

        clearExpiredPenaltyIfNeeded(user);

        String penaltyStatus = user.getPenaltyStatus();
        if (penaltyStatus == null || penaltyStatus.trim().isEmpty()) {
            penaltyStatus = PENALTY_STATUS_NORMAL;
        }

        riskStatus.setNoShowCount(user.getNoShowCount() == null ? 0 : user.getNoShowCount());
        riskStatus.setCurrentActiveOrderCount(getCurrentActiveOrderCount(userId));
        riskStatus.setPenaltyStatus(penaltyStatus);
        riskStatus.setPenaltyReason(user.getPenaltyReason());
        riskStatus.setPenaltyEndTime(user.getPenaltyEndTime());
        riskStatus.setFrozenContactNote(user.getFrozenContactNote());

        if (PENALTY_STATUS_BLOCKED_3D.equals(penaltyStatus)) {
            riskStatus.setRestrictionLevel(PENALTY_STATUS_BLOCKED_3D);
            riskStatus.setRestrictionMessage("你已有1次超时未取餐记录，已限制下单3天，请在" + formatPenaltyEndTime(user.getPenaltyEndTime()) + "后再试" + buildPenaltyContactSuffix(user));
        } else if (PENALTY_STATUS_BLOCKED_7D.equals(penaltyStatus)) {
            riskStatus.setRestrictionLevel(PENALTY_STATUS_BLOCKED_7D);
            riskStatus.setRestrictionMessage("你已有2次超时未取餐记录，已限制下单7天，请在" + formatPenaltyEndTime(user.getPenaltyEndTime()) + "后再试" + buildPenaltyContactSuffix(user));
        } else if (PENALTY_STATUS_BLOCKED_30D.equals(penaltyStatus)) {
            riskStatus.setRestrictionLevel(PENALTY_STATUS_BLOCKED_30D);
            riskStatus.setRestrictionMessage("你已有3次超时未取餐记录，已限制下单30天，请在" + formatPenaltyEndTime(user.getPenaltyEndTime()) + "后再试" + buildPenaltyContactSuffix(user));
        } else if (PENALTY_STATUS_FROZEN.equals(penaltyStatus)) {
            riskStatus.setRestrictionLevel(PENALTY_STATUS_FROZEN);
            riskStatus.setRestrictionMessage("你因多次超时未取餐，预约权限已被冻结" + buildPenaltyContactSuffix(user));
        } else if (riskStatus.getCurrentActiveOrderCount() >= MAX_ACTIVE_ORDER_COUNT) {
            riskStatus.setRestrictionLevel(RESTRICTION_LEVEL_ACTIVE_ORDER_LIMIT);
            riskStatus.setRestrictionMessage("为避免食物浪费，你当前最多只能同时保留2个进行中订单，请先完成其中一个订单后再下单");
        }

        return riskStatus;
    }

    @GetMapping("/client/orders/risk-status")
    public Result<OrderRiskStatus> getOrderRiskStatus() {
        Long userId = getCurrentUserId();
        if (userId == null) {
            return Result.error("未登录或登录已过期");
        }

        return Result.success(buildOrderRiskStatus(userId));
    }

    @GetMapping("/client/orders/daily-integrity-stats")
    public Result<DailyIntegrityStats> getDailyIntegrityStats() {
        return Result.success(buildDailyIntegrityStats());
    }

    @PostMapping("/client/orders")
    @Transactional
    public Result<Order> createOrder(
            @RequestBody OrderRequest request,
            @RequestHeader(value = "X-Client-Request-Id", required = false) String headerClientRequestId
    ) {
        Long userId = getCurrentUserId();
        if (userId == null) {
            return Result.error("未登录或登录已过期");
        }

        if (request == null || request.getShopId() == null) {
            return Result.error("下单参数不完整");
        }

        String clientRequestId = normalizeClientRequestId(request.getClientRequestId());
        if (clientRequestId == null) {
            clientRequestId = normalizeClientRequestId(headerClientRequestId);
        }
        request.setClientRequestId(clientRequestId);
        Order existingOrder = findExistingOrderByClientRequestId(userId, clientRequestId);
        if (existingOrder != null) {
            return Result.success(existingOrder);
        }

        OrderRiskStatus riskStatus = buildOrderRiskStatus(userId);
        if (!PENALTY_STATUS_NORMAL.equals(riskStatus.getRestrictionLevel())) {
            return Result.error(riskStatus.getRestrictionMessage());
        }

        // 1. 验证店铺
        Shop shop = shopMapper.selectById(request.getShopId());
        if (shop != null && !merchantBillingService.isOrderingAllowed(shop.getId())) {
            return Result.error("商户服务暂停，暂时无法下单");
        }
        if (shop == null || shop.getStatus() == 0) {
            return Result.error("店铺暂未营业");
        }

        // 验证取餐时间
        if (request.getPickupTime() == null) {
            return Result.error("请选择取餐时间");
        }
        LocalTime now = LocalTime.now();
        if (now.isBefore(RESERVATION_OPEN_TIME)) {
            return Result.error("每日07:30后开放预约");
        }
        LocalTime pickupTime = request.getPickupTime();
        if (pickupTime.getMinute() % TIME_SLOT_STEP_MINUTES != 0) {
            return Result.error("取餐时间需按5分钟档选择");
        }
        int requiredAdvanceMinutes = reservationRuleService.getRequiredAdvanceMinutes(shop, LocalDate.now(), pickupTime);
        LocalTime earliestPickupTime = roundUpToStep(now.plusMinutes(requiredAdvanceMinutes), TIME_SLOT_STEP_MINUTES);
        if (pickupTime.isBefore(earliestPickupTime)) {
            return Result.error("当前时段最早可预约" + earliestPickupTime.toString().substring(0, 5) + "取餐");
        }
        
        // 简单校验：必须在第一段或第二段营业时间内
        boolean inSlot1 = false;
        if (shop.getOpenTime1() != null && shop.getCloseTime1() != null) {
            inSlot1 = !pickupTime.isBefore(shop.getOpenTime1()) && !pickupTime.isAfter(shop.getCloseTime1());
        }
        
        boolean inSlot2 = false;
        if (shop.getOpenTime2() != null && shop.getCloseTime2() != null) {
            inSlot2 = !pickupTime.isBefore(shop.getOpenTime2()) && !pickupTime.isAfter(shop.getCloseTime2());
        }
        
        if (!inSlot1 && !inSlot2) {
             return Result.error("取餐时间不在营业范围内");
        }

        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal estimatedAmount = null;
        Integer estimatedWeightG = 0;
        BigDecimal brothExtraPrice = BigDecimal.ZERO;
        ShopBrothOption brothOption = null;
        List<OrderItem> orderItems = new ArrayList<>();
        Map<OrderItem, List<OrderItemOption>> orderItemOptionMap = new LinkedHashMap<>();
        List<OrderWeightItem> orderWeightItems = new ArrayList<>();

        if (isWeightSelectionShop(shop)) {
            if (request.getWeightItems() == null || request.getWeightItems().isEmpty()) {
                return Result.error("请选择食材");
            }
            if (request.getBrothOptionId() == null) {
                return Result.error("请选择汤底口味");
            }

            brothOption = shopBrothOptionMapper.selectById(request.getBrothOptionId());
            if (brothOption == null || !request.getShopId().equals(brothOption.getShopId()) || brothOption.getStatus() == null || brothOption.getStatus() != 1) {
                return Result.error("汤底选项无效");
            }
            brothExtraPrice = brothOption.getExtraPrice() == null ? BigDecimal.ZERO : brothOption.getExtraPrice();

            for (WeightItemRequest itemReq : request.getWeightItems()) {
                if (itemReq == null || itemReq.getIngredientId() == null) {
                    return Result.error("食材信息不完整");
                }
                if (itemReq.getQuantity() == null || itemReq.getQuantity() <= 0) {
                    return Result.error("食材数量必须大于0");
                }
                WeightIngredient ingredient = weightIngredientMapper.selectById(itemReq.getIngredientId());
                if (ingredient == null || !request.getShopId().equals(ingredient.getShopId()) || ingredient.getStatus() == null || ingredient.getStatus() != 1) {
                    return Result.error("部分食材已下架，请重新选择");
                }

                int itemWeight = ingredient.getReferenceWeightG() * itemReq.getQuantity();
                estimatedWeightG += itemWeight;

                OrderWeightItem orderWeightItem = new OrderWeightItem();
                orderWeightItem.setIngredientId(ingredient.getId());
                orderWeightItem.setQuantity(itemReq.getQuantity());
                orderWeightItem.setReferenceWeightG(ingredient.getReferenceWeightG());
                orderWeightItem.setEstimatedWeightG(itemWeight);
                orderWeightItems.add(orderWeightItem);
            }

            int minimumOrderWeightG = shop.getMinimumOrderWeightG() == null ? 0 : shop.getMinimumOrderWeightG();
            if (minimumOrderWeightG > 0 && estimatedWeightG < minimumOrderWeightG) {
                return Result.error("当前预计重量不足" + minimumOrderWeightG + "g，请继续选料");
            }

            if (shop.getWeightPricePer500g() == null || shop.getWeightPricePer500g().compareTo(BigDecimal.ZERO) <= 0) {
                return Result.error("店铺暂未配置称重价格");
            }

            estimatedAmount = calculateWeightSelectionAmount(estimatedWeightG, shop.getWeightPricePer500g(), brothExtraPrice, request.getNeedPack());
            totalAmount = estimatedAmount;
        } else {
            if (request.getItems() == null || request.getItems().isEmpty()) {
                return Result.error("订单项不能为空");
            }
            for (OrderItemRequest itemReq : request.getItems()) {
                if (itemReq == null || itemReq.getDishId() == null) {
                    return Result.error("订单项信息不完整");
                }
                if (itemReq.getQuantity() == null || itemReq.getQuantity() <= 0) {
                    return Result.error("菜品数量必须大于0");
                }

                Dish dish = dishMapper.selectById(itemReq.getDishId());
                if (dish == null || dish.getStatus() == 0 || !dish.getShopId().equals(request.getShopId())) {
                    return Result.error("菜品 " + (dish != null ? dish.getName() : "未知") + " 已下架");
                }

                BigDecimal[] optionExtraHolder = new BigDecimal[1];
                String[] optionErrorHolder = new String[1];
                List<OrderItemOption> itemOptions = buildOrderItemOptions(dish, itemReq, optionExtraHolder, optionErrorHolder);
                if (optionErrorHolder[0] != null) {
                    return Result.error(optionErrorHolder[0]);
                }

                BigDecimal finalUnitPrice = dish.getPrice().add(optionExtraHolder[0] == null ? BigDecimal.ZERO : optionExtraHolder[0]);
                BigDecimal itemTotal = finalUnitPrice.multiply(new BigDecimal(itemReq.getQuantity()));
                totalAmount = totalAmount.add(itemTotal);

                OrderItem orderItem = new OrderItem();
                orderItem.setDishId(dish.getId());
                orderItem.setDishName(dish.getName());
                orderItem.setPrice(finalUnitPrice);
                orderItem.setQuantity(itemReq.getQuantity());
                orderItems.add(orderItem);
                orderItemOptionMap.put(orderItem, itemOptions);
            }

            if (request.getNeedPack() != null && request.getNeedPack() == 1) {
                totalAmount = totalAmount.add(new BigDecimal("1.00"));
            }
        }

        OrderSubmissionIdempotencyService.Claim idempotencyClaim =
                orderSubmissionIdempotencyService.claim(userId, clientRequestId);
        if (idempotencyClaim.state() == OrderSubmissionIdempotencyService.ClaimState.SUCCESS) {
            Order completedOrder = orderMapper.selectById(idempotencyClaim.orderId());
            if (completedOrder != null) {
                return Result.success(completedOrder);
            }
            orderSubmissionIdempotencyService.discardStaleSuccess(idempotencyClaim);
            idempotencyClaim = null;
        } else if (idempotencyClaim.state() == OrderSubmissionIdempotencyService.ClaimState.PROCESSING) {
            String completedOrderId = orderSubmissionIdempotencyService.awaitCompletedOrderId(
                    idempotencyClaim, Duration.ofSeconds(1));
            Order completedOrder = completedOrderId == null ? null : orderMapper.selectById(completedOrderId);
            if (completedOrder == null) {
                completedOrder = findExistingOrderByClientRequestId(userId, clientRequestId);
            }
            if (completedOrder != null) {
                return Result.success(completedOrder);
            }
            return Result.error("订单正在处理中，请稍后重试");
        }

        AtomicReference<String> committedOrderId = new AtomicReference<>();
        orderSubmissionIdempotencyService.bindToTransaction(idempotencyClaim, committedOrderId);

        // 3. 生成订单号和取餐号
        LocalDateTime createTime = LocalDateTime.now();
        LocalDate bizDate = createTime.toLocalDate();
        OrderIdGeneratorService.GeneratedOrderNumbers generatedOrderNumbers =
                orderIdGeneratorService.generateOrderNumbers(request.getShopId(), bizDate);

        // 4. 创建订单主记录
        Order order = new Order();
        order.setId(generatedOrderNumbers.getOrderId());
        order.setShopId(request.getShopId());
        order.setBizDate(generatedOrderNumbers.getBizDate());
        order.setUserId(userId); // Use userId
        order.setClientRequestId(clientRequestId);
        order.setTotalAmount(totalAmount);
        order.setPickupCode(generatedOrderNumbers.getPickupCode());
        order.setPickupTime(pickupTime);
        order.setNeedPack(request.getNeedPack() != null ? request.getNeedPack() : 0);
        order.setRemark(normalizeOrderRemark(request.getRemark()));
        order.setOrderMode(isWeightSelectionShop(shop) ? ORDER_MODE_WEIGHT_SELECTION : ORDER_MODE_FIXED_DISH);
        order.setPricingStatus(isWeightSelectionShop(shop) ? PRICING_STATUS_PENDING_CONFIRM : PRICING_STATUS_NOT_REQUIRED);
        order.setEstimatedWeightG(estimatedWeightG);
        order.setEstimatedAmount(estimatedAmount);
        order.setBrothOptionId(brothOption != null ? brothOption.getId() : null);
        order.setBrothName(brothOption != null ? brothOption.getName() : null);
        order.setBrothExtraPrice(brothExtraPrice);
        order.setStatus("making");
        order.setCreateTime(createTime);
        try {
            orderMapper.insert(order);
        } catch (DuplicateKeyException duplicateKeyException) {
            Order duplicatedOrder = findExistingOrderByClientRequestId(userId, clientRequestId);
            if (duplicatedOrder != null) {
                committedOrderId.set(duplicatedOrder.getId());
                return Result.success(duplicatedOrder);
            }
            throw duplicateKeyException;
        }

        // 5. 插入订单详情
        if (ORDER_MODE_WEIGHT_SELECTION.equals(order.getOrderMode())) {
            for (OrderWeightItem orderWeightItem : orderWeightItems) {
                orderWeightItem.setOrderId(generatedOrderNumbers.getOrderId());
                orderWeightItemMapper.insert(orderWeightItem);
            }
        } else {
            for (OrderItem orderItem : orderItems) {
                orderItem.setOrderId(generatedOrderNumbers.getOrderId());
                orderItemMapper.insert(orderItem);
                List<OrderItemOption> itemOptions = orderItemOptionMap.getOrDefault(orderItem, new ArrayList<>());
                for (OrderItemOption itemOption : itemOptions) {
                    itemOption.setOrderItemId(orderItem.getId());
                    itemOption.setCreateTime(LocalDateTime.now());
                    orderItemOptionMapper.insert(itemOption);
                }
            }
        }

        committedOrderId.set(order.getId());
        return Result.success(order);
    }

    @GetMapping("/client/orders")
    public Result<List<Map<String, Object>>> getUserOrders() { // Removed openid param
        Long userId = getCurrentUserId();
        if (userId == null) {
            return Result.error("未登录或登录已过期");
        }

        List<Order> orders = orderMapper.selectList(new LambdaQueryWrapper<Order>()
                .eq(Order::getUserId, userId)
                .orderByDesc(Order::getCreateTime));
        
        List<Map<String, Object>> resultList = new ArrayList<>();
        
        for (Order order : orders) {
            Map<String, Object> map = new java.util.HashMap<>();
            map.put("id", order.getId());
            map.put("shopId", order.getShopId());
            map.put("status", order.getStatus());
            map.put("orderMode", order.getOrderMode());
            map.put("pricingStatus", order.getPricingStatus());
            map.put("pickupCode", order.getPickupCode());
            map.put("pickupTime", order.getPickupTime());
            map.put("needPack", order.getNeedPack());
            map.put("totalAmount", order.getTotalAmount());
            map.put("estimatedAmount", order.getEstimatedAmount());
            map.put("finalAmount", order.getFinalAmount());
            map.put("brothName", order.getBrothName());
            map.put("createTime", order.getCreateTime());
            
            // Fetch Shop Info
            Shop shop = shopMapper.selectById(order.getShopId());
            if (shop != null) {
                map.put("shopName", shop.getName());
                map.put("shopAddress", shop.getAddress());
                map.put("shopContactPhone", shop.getContactPhone());
            }
            
            map.put("items", buildOrderDisplayItems(order));
            map.put("remark", order.getRemark());
            
            resultList.add(map);
        }
        
        return Result.success(resultList);
    }

    @GetMapping("/client/orders/{orderId}")
    public Result<Map<String, Object>> getOrderDetail(@PathVariable String orderId) {
        Long userId = getCurrentUserId();
        if (userId == null) {
            return Result.error("未登录或登录已过期");
        }

        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            return Result.error("订单不存在");
        }
        if (!userId.equals(order.getUserId())) {
            return Result.error("无权查看该订单");
        }
        
        Shop shop = shopMapper.selectById(order.getShopId());
        
        Map<String, Object> result = new java.util.HashMap<>();
        result.put("order", order);
        result.put("items", buildOrderDisplayItems(order));
        result.put("shop", shop);
        result.put("shopName", shop != null ? shop.getName() : "");
        result.put("tasteSensitiveEnabled", isTasteSensitiveShop(shop));
        
        return Result.success(result);
    }

    @GetMapping("/merchant/orders")
    public Result<List<Map<String, Object>>> getMerchantOrders(@RequestParam(required = false) String status) {
        Long shopId = getCurrentMerchantShopId();
        if (shopId == null) {
            return Result.error("未登录或登录已过期");
        }

        LambdaQueryWrapper<Order> queryWrapper = new LambdaQueryWrapper<Order>()
                .eq(Order::getShopId, shopId);
        
        if (status != null && !status.isEmpty()) {
            if ("completed".equals(status)) {
                queryWrapper.in(Order::getStatus, Arrays.asList("completed", "cancelled"));
            } else {
                queryWrapper.eq(Order::getStatus, status);
            }

            if ("making".equals(status) || "pending".equals(status)) {
                queryWrapper.eq(Order::getBizDate, LocalDate.now());
            }
            
            // 排序策略
            if ("making".equals(status)) {
                // 制作中：按取餐时间升序（越早取餐越优先）
                queryWrapper.orderByAsc(Order::getPickupTime);
            } else if ("pending".equals(status)) {
                // 待取餐：按取餐时间升序
                queryWrapper.orderByAsc(Order::getPickupTime);
            } else if ("completed".equals(status)) {
                // 已完成列表同时包含 completed 和 cancelled，先按创建时间兜底查询
                queryWrapper.orderByDesc(Order::getCreateTime);
            } else {
                queryWrapper.orderByDesc(Order::getCreateTime);
            }
        } else {
            queryWrapper.orderByDesc(Order::getCreateTime);
        }
        
        List<Order> orders = orderMapper.selectList(queryWrapper);
        if ("completed".equals(status)) {
            orders.sort((left, right) -> resolveMerchantCompletedSortTime(right)
                    .compareTo(resolveMerchantCompletedSortTime(left)));
        }
        
        // Convert to Map to include items and remark
        List<Map<String, Object>> resultList = new ArrayList<>();
        for (Order order : orders) {
            Map<String, Object> map = new java.util.HashMap<>();
            // Copy properties manually or use BeanUtils
            map.put("id", order.getId());
            map.put("pickupCode", order.getPickupCode());
            map.put("createTime", order.getCreateTime());
            map.put("updateTime", order.getUpdateTime());
            map.put("completedTime", order.getCompletedTime());
            map.put("cancelTime", order.getCancelTime());
            map.put("cancelReason", order.getCancelReason());
            
            // Format pickupTime to HH:mm string to avoid HH:mm:ss
            if (order.getPickupTime() != null) {
                map.put("pickupTime", order.getPickupTime().toString().substring(0, 5));
            } else {
                map.put("pickupTime", null);
            }
            
            map.put("totalAmount", order.getTotalAmount());
            map.put("needPack", order.getNeedPack());
            map.put("status", order.getStatus());
            map.put("orderMode", order.getOrderMode());
            map.put("pricingStatus", order.getPricingStatus());
            map.put("estimatedWeightG", order.getEstimatedWeightG());
            map.put("estimatedAmount", order.getEstimatedAmount());
            map.put("finalAmount", order.getFinalAmount());
            map.put("brothName", order.getBrothName());
            map.put("brothExtraPrice", order.getBrothExtraPrice());
            map.put("remark", order.getRemark());
            map.put("items", buildOrderDisplayItems(order));
            
            resultList.add(map);
        }
        
        return Result.success(resultList);
    }

    @GetMapping("/merchant/orders/history")
    public Result<Map<String, Object>> getMerchantOrderHistory(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long pageSize) {
        Long shopId = getCurrentMerchantShopId();
        if (shopId == null) {
            return Result.error("未登录或登录已过期");
        }

        long normalizedPage = Math.max(1, page);
        long normalizedPageSize = Math.min(100, Math.max(1, pageSize));
        Page<Order> orderPage = orderMapper.selectPage(
                new Page<>(normalizedPage, normalizedPageSize),
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getShopId, shopId)
                        .in(Order::getStatus, Arrays.asList("completed", "cancelled"))
                        .orderByDesc(Order::getClosedTime)
                        .orderByDesc(Order::getId)
        );

        List<Map<String, Object>> records = buildMerchantOrderMaps(orderPage.getRecords());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("records", records);
        result.put("total", orderPage.getTotal());
        result.put("page", normalizedPage);
        result.put("pageSize", normalizedPageSize);
        result.put("totalPages", orderPage.getPages());
        return Result.success(result);
    }

    @GetMapping("/merchant/orders/stale")
    public Result<Map<String, Object>> getMerchantStaleOrders(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long pageSize) {
        Long shopId = getCurrentMerchantShopId();
        if (shopId == null) {
            return Result.error("未登录或登录已过期");
        }

        long normalizedPage = Math.max(1, page);
        long normalizedPageSize = Math.min(100, Math.max(1, pageSize));
        Page<Order> orderPage = orderMapper.selectPage(
                new Page<>(normalizedPage, normalizedPageSize),
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getShopId, shopId)
                        .lt(Order::getBizDate, LocalDate.now())
                        .in(Order::getStatus, Arrays.asList("making", "pending"))
                        .orderByAsc(Order::getBizDate)
                        .orderByAsc(Order::getCreateTime)
                        .orderByAsc(Order::getId)
        );

        List<Map<String, Object>> records = buildMerchantOrderMaps(orderPage.getRecords());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("records", records);
        result.put("total", orderPage.getTotal());
        result.put("page", normalizedPage);
        result.put("pageSize", normalizedPageSize);
        result.put("totalPages", orderPage.getPages());
        return Result.success(result);
    }

    private List<Map<String, Object>> buildMerchantOrderMaps(List<Order> orders) {
        if (orders == null || orders.isEmpty()) {
            return new ArrayList<>();
        }

        List<String> orderIds = orders.stream().map(Order::getId).collect(Collectors.toList());
        Map<String, List<Map<String, Object>>> displayItemsByOrderId = buildOrderDisplayItemsBatch(orderIds);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Order order : orders) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", order.getId());
            map.put("bizDate", order.getBizDate());
            map.put("pickupCode", order.getPickupCode());
            map.put("createTime", order.getCreateTime());
            map.put("updateTime", order.getUpdateTime());
            map.put("completedTime", order.getCompletedTime());
            map.put("readyTime", order.getReadyTime());
            map.put("closedTime", order.getClosedTime());
            map.put("cancelTime", order.getCancelTime());
            map.put("cancelReason", order.getCancelReason());
            map.put("pickupTime", order.getPickupTime() == null
                    ? null
                    : order.getPickupTime().toString().substring(0, 5));
            map.put("totalAmount", order.getTotalAmount());
            map.put("needPack", order.getNeedPack());
            map.put("status", order.getStatus());
            map.put("orderMode", order.getOrderMode());
            map.put("pricingStatus", order.getPricingStatus());
            map.put("estimatedWeightG", order.getEstimatedWeightG());
            map.put("estimatedAmount", order.getEstimatedAmount());
            map.put("finalAmount", order.getFinalAmount());
            map.put("brothName", order.getBrothName());
            map.put("brothExtraPrice", order.getBrothExtraPrice());
            map.put("remark", order.getRemark());
            map.put("items", displayItemsByOrderId.getOrDefault(order.getId(), Collections.emptyList()));
            result.add(map);
        }
        return result;
    }

    @Transactional
    @PutMapping("/merchant/orders/{orderId}/resolve-stale")
    public Result<String> resolveMerchantStaleOrder(
            @PathVariable String orderId,
            @RequestBody ResolveStaleOrderRequest request) {
        Long shopId = getCurrentMerchantShopId();
        if (shopId == null) {
            return Result.error("未登录或登录已过期");
        }
        if (request == null || request.getResult() == null) {
            return Result.error("请选择处理结果");
        }

        Order order = orderMapper.selectByIdForUpdate(orderId);
        if (order == null) {
            return Result.error("订单不存在");
        }
        if (!shopId.equals(order.getShopId())) {
            return Result.error("无权操作该订单");
        }
        if (order.getBizDate() == null || !order.getBizDate().isBefore(LocalDate.now())) {
            return Result.error("该订单不是隔夜遗留订单");
        }
        if (!"making".equals(order.getStatus()) && !"pending".equals(order.getStatus())) {
            return Result.error("订单状态已发生变化，请刷新后重试");
        }

        String resolution = request.getResult().trim();
        LocalDateTime now = LocalDateTime.now();
        if ("completed".equals(resolution)) {
            order.setStatus("completed");
            order.setCompletedTime(now);
            order.setClosedTime(now);
            order.setPickupOvertimeMinutes(calculatePickupOvertimeMinutes(order, now));
            order.setPickupOvertimeNote(buildPickupOvertimeNote(order, order.getPickupOvertimeMinutes(), now));
        } else if ("no_show".equals(resolution)) {
            String noShowReason = "用户超时未取";
            order.setStatus("cancelled");
            order.setCancelReason(noShowReason);
            order.setCancelTime(now);
            order.setClosedTime(now);
            applyNoShowPenalty(order, noShowReason, now);
        } else if ("cancelled".equals(resolution)) {
            String cancelReason = request.getCancelReason() == null ? "" : request.getCancelReason().trim();
            if (cancelReason.isEmpty()) {
                return Result.error("请填写取消原因");
            }
            if (isUserNoShowCancelReason(cancelReason)) {
                return Result.error("用户未取餐请使用逃单处理");
            }
            order.setStatus("cancelled");
            order.setCancelReason(cancelReason);
            order.setCancelTime(now);
            order.setClosedTime(now);
        } else {
            return Result.error("不支持的处理结果");
        }

        order.setUpdateTime(now);
        if (orderMapper.updateById(order) != 1) {
            throw new IllegalStateException("遗留订单状态更新失败");
        }
        return Result.success("遗留订单已处理");
    }

    private Map<String, List<Map<String, Object>>> buildOrderDisplayItemsBatch(List<String> orderIds) {
        Map<String, List<Map<String, Object>>> result = new LinkedHashMap<>();
        orderIds.forEach(orderId -> result.put(orderId, new ArrayList<>()));

        List<OrderItem> items = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                .in(OrderItem::getOrderId, orderIds));
        Map<Long, List<OrderItemOption>> optionMap = getOrderItemOptionMap(
                items.stream().map(OrderItem::getId).collect(Collectors.toList()));
        Set<Long> dishIds = items.stream().map(OrderItem::getDishId).filter(java.util.Objects::nonNull).collect(Collectors.toSet());
        Map<Long, Dish> dishMap = dishIds.isEmpty()
                ? new LinkedHashMap<>()
                : dishMapper.selectBatchIds(dishIds).stream().collect(Collectors.toMap(Dish::getId, dish -> dish));

        for (OrderItem item : items) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("dishName", item.getDishName());
            map.put("quantity", item.getQuantity());
            map.put("price", item.getPrice());
            Dish dish = dishMap.get(item.getDishId());
            map.put("image", dish != null ? dish.getImage() : null);
            List<OrderItemOption> options = optionMap.getOrDefault(item.getId(), Collections.emptyList());
            if (!options.isEmpty()) {
                List<Map<String, Object>> optionDTOs = new ArrayList<>();
                for (OrderItemOption option : options) {
                    Map<String, Object> optionDTO = new LinkedHashMap<>();
                    optionDTO.put("groupName", option.getGroupName());
                    optionDTO.put("valueName", option.getValueName());
                    optionDTO.put("extraPrice", option.getExtraPrice());
                    optionDTOs.add(optionDTO);
                }
                map.put("optionList", optionDTOs);
                map.put("options", buildOptionSummary(options));
            }
            result.computeIfAbsent(item.getOrderId(), key -> new ArrayList<>()).add(map);
        }

        List<OrderWeightItem> weightItems = orderWeightItemMapper.selectList(new LambdaQueryWrapper<OrderWeightItem>()
                .in(OrderWeightItem::getOrderId, orderIds));
        Set<Long> ingredientIds = weightItems.stream()
                .map(OrderWeightItem::getIngredientId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, WeightIngredient> ingredientMap = ingredientIds.isEmpty()
                ? new LinkedHashMap<>()
                : weightIngredientMapper.selectBatchIds(ingredientIds).stream()
                        .collect(Collectors.toMap(WeightIngredient::getId, ingredient -> ingredient));
        for (OrderWeightItem weightItem : weightItems) {
            WeightIngredient ingredient = ingredientMap.get(weightItem.getIngredientId());
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("dishName", ingredient != null ? ingredient.getName() : "已删除食材");
            map.put("quantity", weightItem.getQuantity());
            map.put("price", null);
            map.put("image", ingredient != null ? ingredient.getImage() : null);
            result.computeIfAbsent(weightItem.getOrderId(), key -> new ArrayList<>()).add(map);
        }
        return result;
    }

    private LocalDateTime resolveMerchantCompletedSortTime(Order order) {
        if ("cancelled".equals(order.getStatus()) && order.getCancelTime() != null) {
            return order.getCancelTime();
        }
        if (order.getCompletedTime() != null) {
            return order.getCompletedTime();
        }
        if (order.getCancelTime() != null) {
            return order.getCancelTime();
        }
        if (order.getUpdateTime() != null) {
            return order.getUpdateTime();
        }
        return order.getCreateTime();
    }

    @GetMapping("/merchant/orders/{orderId}")
    public Result<Map<String, Object>> getMerchantOrderDetail(@PathVariable String orderId) {
        Long shopId = getCurrentMerchantShopId();
        if (shopId == null) {
            return Result.error("未登录或登录已过期");
        }

        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            return Result.error("订单不存在");
        }
        if (!shopId.equals(order.getShopId())) {
            return Result.error("无权查看该订单");
        }
        
        // Convert to Map
        Map<String, Object> map = new java.util.HashMap<>();
        map.put("id", order.getId());
        map.put("pickupCode", order.getPickupCode());
        map.put("createTime", order.getCreateTime());
        map.put("updateTime", order.getUpdateTime());
        map.put("readyTime", order.getReadyTime());
        map.put("completedTime", order.getCompletedTime());
        map.put("secondPickupReminderTime", order.getSecondPickupReminderTime());
        map.put("pickupOvertimeMinutes", order.getPickupOvertimeMinutes());
        map.put("pickupOvertimeNote", order.getPickupOvertimeNote());
        
        // Format pickupTime to HH:mm string to avoid HH:mm:ss
        if (order.getPickupTime() != null) {
            map.put("pickupTime", order.getPickupTime().toString().substring(0, 5));
        } else {
            map.put("pickupTime", null);
        }
        
        map.put("totalAmount", order.getTotalAmount());
        map.put("needPack", order.getNeedPack());
        map.put("status", order.getStatus());
        map.put("orderMode", order.getOrderMode());
        map.put("pricingStatus", order.getPricingStatus());
        map.put("estimatedWeightG", order.getEstimatedWeightG());
        map.put("estimatedAmount", order.getEstimatedAmount());
        map.put("finalWeightG", order.getFinalWeightG());
        map.put("finalAmount", order.getFinalAmount());
        map.put("priceConfirmTime", order.getPriceConfirmTime());
        map.put("priceEvidenceImage", order.getPriceEvidenceImage());
        map.put("brothName", order.getBrothName());
        map.put("brothExtraPrice", order.getBrothExtraPrice());
        map.put("cancelReason", order.getCancelReason());
        map.put("cancelTime", order.getCancelTime());
        
        // Fetch User Info
        User user = null;
        if (order.getUserId() != null) {
            user = userMapper.selectById(order.getUserId());
            if (user != null) {
                map.put("userName", user.getName());
                map.put("userPhone", user.getPhone());
            }
        }
        
        map.put("remark", order.getRemark());
        map.put("items", buildOrderDisplayItems(order));
        
        return Result.success(map);
    }

    @PutMapping("/merchant/orders/{orderId}/prepare")
    public Result<String> prepareOrder(@PathVariable String orderId) {
        Long shopId = getCurrentMerchantShopId();
        if (shopId == null) {
            return Result.error("未登录或登录已过期");
        }

        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            return Result.error("订单不存在");
        }
        
        if (!shopId.equals(order.getShopId())) {
            return Result.error("无权操作该订单");
        }
        
        // 允许从 making 转为 pending
        // 如果已经是 pending，也允许 idempotent 操作
        if (!"making".equals(order.getStatus()) && !"pending".equals(order.getStatus())) {
            return Result.error("订单状态不正确，当前状态：" + order.getStatus());
        }
        if (ORDER_MODE_WEIGHT_SELECTION.equals(order.getOrderMode()) && !PRICING_STATUS_CONFIRMED.equals(order.getPricingStatus())) {
            return Result.error("请先确认最终重量和金额");
        }
        
        // 记录原始状态，用于判断是否需要发送订阅消息
        String originalStatus = order.getStatus();
        System.out.println("===== 订单制作完成状态变更 =====");
        System.out.println("订单ID: " + orderId);
        System.out.println("原始状态: " + originalStatus + ", 新状态: pending");
        
        order.setStatus("pending");
        LocalDateTime now = LocalDateTime.now();
        if ("making".equals(originalStatus) && order.getReadyTime() == null) {
            order.setReadyTime(now);
        }
        order.setUpdateTime(now);
        orderMapper.updateById(order);
        
        // 如果状态从making变为pending，发送取餐提醒订阅消息
        if ("making".equals(originalStatus)) {
            System.out.println("触发订阅消息发送: making -> pending");
            sendPickupReminder(orderId);
        } else {
            System.out.println("不触发订阅消息发送: 原始状态为 " + originalStatus + "，非making状态");
        }
        
        System.out.println("===== 订单制作完成处理结束 =====");
        return Result.success("订单制作完成");
    }

    @PutMapping("/merchant/orders/{orderId}/confirm-price")
    public Result<String> confirmWeightOrderPrice(@PathVariable String orderId, @RequestBody ConfirmWeightOrderRequest request) {
        Long shopId = getCurrentMerchantShopId();
        if (shopId == null) {
            return Result.error("未登录或登录已过期");
        }
        if (request == null || request.getFinalWeightG() == null || request.getFinalWeightG() <= 0) {
            return Result.error("请输入有效的最终重量");
        }
        if (request.getFinalAmount() == null || request.getFinalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return Result.error("请输入有效的最终金额");
        }

        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            return Result.error("订单不存在");
        }
        if (!shopId.equals(order.getShopId())) {
            return Result.error("无权操作该订单");
        }
        if (!ORDER_MODE_WEIGHT_SELECTION.equals(order.getOrderMode())) {
            return Result.error("当前订单不是自选称重订单");
        }
        if (!"making".equals(order.getStatus())) {
            return Result.error("当前订单状态不允许确认金额");
        }

        String normalizedPriceEvidenceImage = normalizeOrderRemark(request.getPriceEvidenceImage());
        if (isPriceEvidenceRequired(order.getEstimatedWeightG(), request.getFinalWeightG()) && normalizedPriceEvidenceImage == null) {
            int estimatedWeight = order.getEstimatedWeightG() == null ? 0 : order.getEstimatedWeightG();
            int tolerance = calculateWeightEvidenceTolerance(estimatedWeight);
            return Result.error("最终重量超出预计范围，请上传称重凭证图片后再确认（预计" + estimatedWeight + "g，超出" + tolerance + "g以上需留证）");
        }

        LocalDateTime now = LocalDateTime.now();
        order.setPricingStatus(PRICING_STATUS_CONFIRMED);
        order.setFinalWeightG(request.getFinalWeightG());
        order.setFinalAmount(request.getFinalAmount());
        order.setTotalAmount(request.getFinalAmount());
        order.setPriceEvidenceImage(normalizedPriceEvidenceImage);
        order.setPriceConfirmTime(now);
        order.setUpdateTime(now);
        orderMapper.updateById(order);
        return Result.success("最终金额已确认");
    }

    @PutMapping("/merchant/orders/{orderId}/complete")
    public Result<String> completeOrder(@PathVariable String orderId) {
        Long shopId = getCurrentMerchantShopId();
        if (shopId == null) {
            return Result.error("未登录或登录已过期");
        }

        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            return Result.error("订单不存在");
        }
        
        if (!shopId.equals(order.getShopId())) {
            return Result.error("无权操作该订单");
        }
        
        LocalDateTime completedTime = LocalDateTime.now();
        int overtimeMinutes = calculatePickupOvertimeMinutes(order, completedTime);

        order.setStatus("completed");
        order.setCompletedTime(completedTime);
        order.setClosedTime(completedTime);
        order.setPickupOvertimeMinutes(overtimeMinutes);
        order.setPickupOvertimeNote(buildPickupOvertimeNote(order, overtimeMinutes, completedTime));
        order.setUpdateTime(completedTime);
        orderMapper.updateById(order);
        
        return Result.success("订单已完成");
    }

    /**
     * 商户取消订单（如菜品售罄等）
     * @param orderId 订单ID
     * @param cancelReason 取消原因
     * @return 操作结果
     */
    @PutMapping("/merchant/orders/{orderId}/cancel")
    public Result<String> cancelOrder(@PathVariable String orderId, @RequestBody(required = false) Map<String, String> requestBody) {
        Long shopId = getCurrentMerchantShopId();
        if (shopId == null) {
            return Result.error("未登录或登录已过期");
        }

        String cancelReason = requestBody != null ? requestBody.get("cancelReason") : null;
        
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            return Result.error("订单不存在");
        }
        
        if (!shopId.equals(order.getShopId())) {
            return Result.error("无权操作该订单");
        }
        
        // 检查订单状态是否允许取消（pending或making状态可以取消）
        String status = order.getStatus();
        if (!"pending".equals(status) && !"making".equals(status)) {
            return Result.error("只有待确认和制作中的订单可以取消");
        }
        
        LocalDateTime cancelTime = LocalDateTime.now();

        // 更新订单状态和取消信息
        order.setStatus("cancelled");
        order.setCancelReason(cancelReason);
        order.setCancelTime(cancelTime);
        order.setClosedTime(cancelTime);
        order.setUpdateTime(cancelTime);
        orderMapper.updateById(order);

        applyNoShowPenalty(order, cancelReason, cancelTime);
        
        return Result.success("订单已取消");
    }

    /**
     * 发送取餐提醒订阅消息
     * @param orderId 订单ID
     */
    private void sendPickupReminder(String orderId) {
        System.out.println("===== 开始处理订单订阅消息发送 =====");
        System.out.println("订单ID: " + orderId);
        
        try {
            Order order = orderMapper.selectById(orderId);
            if (order == null) {
                System.out.println("❌ 订单不存在，订单ID: " + orderId);
                return;
            }
            if (order.getUserId() == null) {
                System.out.println("❌ 订单用户ID为空，订单ID: " + orderId);
                return;
            }
            
            System.out.println("订单信息: ID=" + order.getId() + 
                             ", 状态=" + order.getStatus() + 
                             ", 用户ID=" + order.getUserId() + 
                             ", 店铺ID=" + order.getShopId() + 
                             ", 取餐号=" + order.getPickupCode() + 
                             ", 取餐时间=" + order.getPickupTime());

            // 获取用户openid
            User user = userMapper.selectById(order.getUserId());
            if (user == null) {
                System.out.println("❌ 用户不存在，用户ID: " + order.getUserId());
                return;
            }
            if (user.getOpenid() == null) {
                System.out.println("❌ 用户openid为空，用户ID: " + order.getUserId() + ", 用户信息: " + user);
                return;
            }
            
            System.out.println("用户信息: ID=" + user.getId() + 
                             ", openid=" + user.getOpenid() + 
                             ", 长度=" + (user.getOpenid() != null ? user.getOpenid().length() : 0));

            // 获取店铺信息
            Shop shop = shopMapper.selectById(order.getShopId());
            if (shop == null) {
                System.out.println("❌ 店铺不存在，店铺ID: " + order.getShopId());
                return;
            }
            
            System.out.println("店铺信息: ID=" + shop.getId() + 
                             ", 名称=" + shop.getName() + 
                             ", 地址=" + shop.getAddress());

            // 格式化取餐时间
            String pickupTimeStr = order.getPickupTime() != null ? 
                order.getPickupTime().toString().substring(0, 5) : "未知时间";
            System.out.println("格式化取餐时间: " + pickupTimeStr + " (原始: " + order.getPickupTime() + ")");

            System.out.println("准备发送订阅消息，参数: openid=" + user.getOpenid() + 
                             ", pickupTime=" + pickupTimeStr + 
                             ", shopName=" + shop.getName() + 
                             ", shopAddress=" + shop.getAddress() + 
                             ", pickupCode=" + order.getPickupCode());

            // 发送订阅消息（异步调用）
            wechatSubscribeMessageService.sendPickupReminder(
                user.getOpenid(),
                pickupTimeStr,
                shop.getName(),
                shop.getAddress(),
                order.getPickupCode(),
                order.getId()
            ).thenAccept(success -> {
                // 记录发送结果日志
                if (success) {
                    System.out.println("✅ 微信订阅消息发送成功，订单ID: " + orderId);
                } else {
                    System.out.println("❌ 微信订阅消息发送失败，订单ID: " + orderId);
                }
            });
        } catch (Exception e) {
            System.out.println("❌ 发送订阅消息异常: " + e.getClass().getName() + " - " + e.getMessage());
            e.printStackTrace();
        }
        System.out.println("===== 订单订阅消息发送处理结束 =====");
    }
}
