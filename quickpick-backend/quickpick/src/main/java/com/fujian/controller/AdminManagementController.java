package com.fujian.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fujian.common.PasswordUtil;
import com.fujian.common.Result;
import com.fujian.mapper.*;
import com.fujian.pojo.*;
import com.fujian.service.AdminAuditService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Data;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@RestController
@RequestMapping("/api/admin")
public class AdminManagementController {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private final JdbcTemplate jdbc;
    private final ShopMapper shopMapper;
    private final CanteenMapper canteenMapper;
    private final UserMapper userMapper;
    private final OrderMapper orderMapper;
    private final FeedbackMapper feedbackMapper;
    private final ReservationRuleConfigMapper reservationMapper;
    private final LuckyDrawConfigMapper luckyDrawMapper;
    private final AdminOperationLogMapper logMapper;
    private final AdminAuditService auditService;

    public AdminManagementController(JdbcTemplate jdbc, ShopMapper shopMapper, CanteenMapper canteenMapper,
                                     UserMapper userMapper, OrderMapper orderMapper, FeedbackMapper feedbackMapper,
                                     ReservationRuleConfigMapper reservationMapper, LuckyDrawConfigMapper luckyDrawMapper,
                                     AdminOperationLogMapper logMapper, AdminAuditService auditService) {
        this.jdbc = jdbc;
        this.shopMapper = shopMapper;
        this.canteenMapper = canteenMapper;
        this.userMapper = userMapper;
        this.orderMapper = orderMapper;
        this.feedbackMapper = feedbackMapper;
        this.reservationMapper = reservationMapper;
        this.luckyDrawMapper = luckyDrawMapper;
        this.logMapper = logMapper;
        this.auditService = auditService;
    }

    @GetMapping("/dashboard")
    public Result<Map<String, Object>> dashboard(@RequestParam(required = false) LocalDate startDate,
                                                  @RequestParam(required = false) LocalDate endDate) {
        LocalDate end = endDate == null ? LocalDate.now() : endDate;
        LocalDate start = startDate == null ? end.minusDays(6) : startDate;
        if (start.isAfter(end) || start.isBefore(end.minusYears(2))) return Result.error("日期范围不合法");
        Timestamp startTime = Timestamp.valueOf(start.atStartOfDay());
        Timestamp endTime = Timestamp.valueOf(end.plusDays(1).atStartOfDay());

        Map<String, Object> summary = jdbc.queryForMap("""
                SELECT COUNT(*) orderCount,
                       SUM(status='completed') completedCount,
                       SUM(status='cancelled') cancelledCount,
                       COALESCE(SUM(CASE WHEN status='completed' THEN total_amount ELSE 0 END),0) revenue,
                       COUNT(DISTINCT user_id) activeUsers,
                       COUNT(DISTINCT shop_id) activeShops
                FROM orders WHERE create_time >= ? AND create_time < ?
                """, startTime, endTime);
        BigDecimal revenue = asDecimal(summary.get("revenue"));
        long completed = asLong(summary.get("completedCount"));
        summary.put("averageOrderValue", completed == 0 ? BigDecimal.ZERO : revenue.divide(BigDecimal.valueOf(completed), 2, java.math.RoundingMode.HALF_UP));

        List<Map<String, Object>> trend = jdbc.queryForList("""
                SELECT DATE(create_time) date, COUNT(*) orderCount,
                       COALESCE(SUM(CASE WHEN status='completed' THEN total_amount ELSE 0 END),0) revenue
                FROM orders WHERE create_time >= ? AND create_time < ?
                GROUP BY DATE(create_time) ORDER BY date
                """, startTime, endTime);
        List<Map<String, Object>> statusDistribution = jdbc.queryForList("""
                SELECT status, COUNT(*) count FROM orders
                WHERE create_time >= ? AND create_time < ? GROUP BY status ORDER BY count DESC
                """, startTime, endTime);
        List<Map<String, Object>> dailyShopTrend = jdbc.queryForList("""
                SELECT DATE(o.create_time) date, s.id shopId, s.name shopName, COUNT(o.id) orderCount
                FROM orders o JOIN shops s ON s.id=o.shop_id
                WHERE o.create_time >= ? AND o.create_time < ?
                GROUP BY DATE(o.create_time),s.id,s.name
                ORDER BY date,s.id
                """, startTime, endTime);
        LocalDate calendarStart = LocalDate.of(end.getYear(), 1, 1);
        List<Map<String, Object>> calendarTrend = jdbc.queryForList("""
                SELECT DATE(create_time) date,COUNT(*) orderCount
                FROM orders WHERE create_time>=? AND create_time<?
                GROUP BY DATE(create_time) ORDER BY date
                """, Timestamp.valueOf(calendarStart.atStartOfDay()), Timestamp.valueOf(calendarStart.plusYears(1).atStartOfDay()));
        List<Map<String, Object>> topShops = jdbc.queryForList("""
                SELECT s.id, s.name, COUNT(o.id) orderCount,
                       COALESCE(SUM(CASE WHEN o.status='completed' THEN o.total_amount ELSE 0 END),0) revenue
                FROM shops s LEFT JOIN orders o ON o.shop_id=s.id AND o.create_time >= ? AND o.create_time < ?
                GROUP BY s.id,s.name ORDER BY revenue DESC,orderCount DESC LIMIT 10
                """, startTime, endTime);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("startDate", start);
        data.put("endDate", end);
        data.put("summary", summary);
        data.put("trend", trend);
        data.put("statusDistribution", statusDistribution);
        data.put("dailyShopTrend", dailyShopTrend);
        data.put("calendarYear", end.getYear());
        data.put("calendarTrend", calendarTrend);
        data.put("topShops", topShops);
        return Result.success(data);
    }

    @GetMapping("/orders")
    public Result<Map<String, Object>> orders(@RequestParam(defaultValue = "1") int page,
                                               @RequestParam(defaultValue = "20") int pageSize,
                                               @RequestParam(required = false) String keyword,
                                               @RequestParam(required = false) Long shopId,
                                               @RequestParam(required = false) Long userId,
                                               @RequestParam(required = false) String status,
                                               @RequestParam(required = false) String orderMode,
                                               @RequestParam(required = false) LocalDate startDate,
                                               @RequestParam(required = false) LocalDate endDate) {
        SqlFilter filter = orderFilter(keyword, shopId, userId, status, orderMode, startDate, endDate);
        String from = " FROM orders o JOIN shops s ON s.id=o.shop_id LEFT JOIN users u ON u.id=o.user_id " + filter.where;
        Long total = jdbc.queryForObject("SELECT COUNT(*)" + from, Long.class, filter.args.toArray());
        List<Object> args = new ArrayList<>(filter.args);
        args.add(safePageSize(pageSize));
        args.add(offset(page, pageSize));
        List<Map<String, Object>> records = jdbc.queryForList("""
                SELECT o.id,o.pickup_code,o.shop_id,s.name shop_name,o.user_id,u.name user_name,u.phone,
                       o.total_amount,o.status,o.order_mode,o.pricing_status,o.pickup_time,o.create_time,
                       o.completed_time,o.cancel_time,o.cancel_reason,o.remark
                """ + from + " ORDER BY o.create_time DESC,o.id DESC LIMIT ? OFFSET ?", args.toArray());
        return Result.success(page(records, total, page, pageSize));
    }

    @GetMapping("/orders/summary")
    public Result<Map<String, Object>> orderSummary() {
        Timestamp today = Timestamp.valueOf(LocalDate.now().atStartOfDay());
        Timestamp tomorrow = Timestamp.valueOf(LocalDate.now().plusDays(1).atStartOfDay());
        Map<String, Object> summary = jdbc.queryForMap("""
                SELECT COUNT(*) todayOrders,
                       SUM(status='making') makingCount,
                       SUM(status='pending') pendingCount,
                       SUM(status='completed') completedCount,
                       SUM(status='cancelled') cancelledCount,
                       COALESCE(SUM(CASE WHEN status='completed' THEN total_amount ELSE 0 END),0) todayRevenue
                FROM orders WHERE create_time>=? AND create_time<?
                """, today, tomorrow);
        summary.put("totalOrders", jdbc.queryForObject("SELECT COUNT(*) FROM orders", Long.class));
        return Result.success(summary);
    }

    @GetMapping("/orders/{id}")
    public Result<Map<String, Object>> orderDetail(@PathVariable String id) {
        List<Map<String, Object>> rows = jdbc.queryForList("""
                SELECT o.*,s.name shop_name,u.name user_name,u.phone user_phone
                FROM orders o JOIN shops s ON s.id=o.shop_id LEFT JOIN users u ON u.id=o.user_id WHERE o.id=?
                """, id);
        if (rows.isEmpty()) return Result.error("订单不存在");
        Map<String, Object> data = new LinkedHashMap<>(rows.get(0));
        List<Map<String, Object>> items = jdbc.queryForList("SELECT * FROM order_items WHERE order_id=? ORDER BY id", id);
        List<Map<String, Object>> options = jdbc.queryForList("""
                SELECT option_row.order_item_id,option_row.group_name,option_row.value_name,option_row.extra_price
                FROM order_item_options option_row JOIN order_items item ON item.id=option_row.order_item_id
                WHERE item.order_id=? ORDER BY option_row.sort,option_row.id
                """, id);
        for (Map<String, Object> item : items) {
            List<Map<String, Object>> itemOptions = options.stream()
                    .filter(option -> Objects.equals(asLong(option.get("order_item_id")), asLong(item.get("id"))))
                    .toList();
            item.put("optionList", itemOptions);
            item.put("options", itemOptions.stream().map(option -> option.get("group_name") + "：" + option.get("value_name")).collect(java.util.stream.Collectors.joining("，")));
        }
        data.put("items", items);
        data.put("weightItems", jdbc.queryForList("""
                SELECT wi.*,w.name ingredient_name FROM order_weight_items wi
                LEFT JOIN weight_ingredients w ON w.id=wi.ingredient_id WHERE wi.order_id=? ORDER BY wi.id
                """, id));
        return Result.success(data);
    }

    @Data
    public static class StatusCorrectionRequest {
        private String targetStatus;
        private String reason;
    }

    @PutMapping("/orders/{id}/status")
    @Transactional
    public Result<Void> correctOrderStatus(@PathVariable String id, @RequestBody StatusCorrectionRequest body,
                                            HttpServletRequest request) {
        Set<String> allowed = Set.of("making", "pending", "completed", "cancelled");
        if (!allowed.contains(body.getTargetStatus()) || isBlank(body.getReason())) return Result.error("目标状态和操作原因不能为空");
        Order order = orderMapper.selectByIdForUpdate(id);
        if (order == null) return Result.error("订单不存在");
        LocalDateTime now = LocalDateTime.now();
        order.setStatus(body.getTargetStatus());
        order.setCancelReason(null);
        order.setCancelTime(null);
        order.setCompletedTime(null);
        if ("cancelled".equals(body.getTargetStatus())) {
            order.setCancelReason("管理员纠正：" + body.getReason().trim());
            order.setCancelTime(now);
            order.setClosedTime(now);
        } else if ("completed".equals(body.getTargetStatus())) {
            order.setCompletedTime(now);
            order.setClosedTime(now);
        } else {
            order.setClosedTime(null);
            if ("pending".equals(body.getTargetStatus()) && order.getReadyTime() == null) order.setReadyTime(now);
        }
        orderMapper.updateById(order);
        auditService.record(adminId(), "CORRECT_ORDER_STATUS", "order", id,
                "改为" + body.getTargetStatus() + "；" + body.getReason().trim(), request);
        return Result.success(null);
    }

    @GetMapping("/shops")
    public Result<Map<String, Object>> shops(@RequestParam(defaultValue = "1") int page,
                                              @RequestParam(defaultValue = "20") int pageSize,
                                              @RequestParam(required = false) String keyword,
                                              @RequestParam(required = false) Integer canteenId) {
        List<Object> args = new ArrayList<>();
        StringBuilder where = new StringBuilder(" WHERE 1=1");
        if (!isBlank(keyword)) { where.append(" AND (s.name LIKE ? OR s.contact_phone LIKE ? OR CAST(s.id AS CHAR)=?)"); String k="%"+keyword.trim()+"%"; args.add(k);args.add(k);args.add(keyword.trim()); }
        if (canteenId != null) { where.append(" AND s.canteen_id=?");args.add(canteenId); }
        Long total = jdbc.queryForObject("SELECT COUNT(*) FROM shops s"+where, Long.class,args.toArray());
        List<Object> pageArgs=new ArrayList<>(args);pageArgs.add(safePageSize(pageSize));pageArgs.add(offset(page,pageSize));
        List<Map<String,Object>> records=jdbc.queryForList("""
                SELECT s.*,c.name canteen_name,(SELECT COUNT(*) FROM orders o WHERE o.shop_id=s.id) order_count
                FROM shops s LEFT JOIN canteens c ON c.id=s.canteen_id
                """+where+" ORDER BY s.display_sort,s.id LIMIT ? OFFSET ?",pageArgs.toArray());
        return Result.success(page(records,total,page,pageSize));
    }

    @GetMapping("/shops/options")
    public Result<List<Map<String, Object>>> shopOptions() {
        return Result.success(jdbc.queryForList("SELECT id,name,canteen_id FROM shops ORDER BY display_sort,id"));
    }

    @PostMapping("/shops")
    @Transactional
    public Result<Map<String, Object>> createShop(@RequestBody Shop body, HttpServletRequest request) {
        if (isBlank(body.getName())) return Result.error("商户名称不能为空");
        if (body.getCanteenId() == null || canteenMapper.selectById(body.getCanteenId()) == null) return Result.error("请选择有效食堂");
        String initialPassword = numericPassword();
        body.setName(body.getName().trim());
        body.setPassword(PasswordUtil.encrypt(initialPassword));
        body.setPwdUpdatedAt(null);
        body.setStatus(body.getStatus() == null ? 1 : body.getStatus());
        body.setVisible(body.getVisible() == null ? 1 : body.getVisible());
        body.setDisplaySort(body.getDisplaySort() == null ? 0 : Math.max(0, body.getDisplaySort()));
        body.setShopMode(isBlank(body.getShopMode()) ? "fixed_dish" : body.getShopMode());
        body.setPeakLimitEnabled(body.getPeakLimitEnabled() == null ? 0 : body.getPeakLimitEnabled());
        body.setTasteSensitiveEnabled(body.getTasteSensitiveEnabled() == null ? 0 : body.getTasteSensitiveEnabled());
        body.setOpenTime1(body.getOpenTime1() == null ? LocalTime.of(8, 0) : body.getOpenTime1());
        body.setCloseTime1(body.getCloseTime1() == null ? LocalTime.of(20, 0) : body.getCloseTime1());
        shopMapper.insert(body);
        auditService.record(adminId(), "CREATE_SHOP", "shop", String.valueOf(body.getId()), "新增商户：" + body.getName(), request);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("shopId", body.getId());
        result.put("initialPassword", initialPassword);
        return Result.success(result);
    }

    @PutMapping("/shops/{id}")
    public Result<Void> updateShop(@PathVariable Long id, @RequestBody Shop body, HttpServletRequest request) {
        Shop shop=shopMapper.selectById(id); if(shop==null)return Result.error("商户不存在");
        if (!isBlank(body.getName())) shop.setName(body.getName().trim());
        shop.setAddress(body.getAddress());
        shop.setContactPhone(body.getContactPhone());
        shop.setLogoImage(body.getLogoImage());
        shop.setCoverImage(body.getCoverImage());
        if (body.getCanteenId() != null) shop.setCanteenId(body.getCanteenId());
        if (body.getStatus() != null) shop.setStatus(body.getStatus());
        if (body.getVisible() != null) shop.setVisible(body.getVisible());
        if (body.getDisplaySort() != null) shop.setDisplaySort(Math.max(0, body.getDisplaySort()));
        if (!isBlank(body.getShopMode())) shop.setShopMode(body.getShopMode());
        if (body.getPeakLimitEnabled() != null) shop.setPeakLimitEnabled(body.getPeakLimitEnabled());
        if (body.getTasteSensitiveEnabled() != null) shop.setTasteSensitiveEnabled(body.getTasteSensitiveEnabled());
        if (body.getOpenTime1() != null) shop.setOpenTime1(body.getOpenTime1());
        if (body.getCloseTime1() != null) shop.setCloseTime1(body.getCloseTime1());
        shop.setOpenTime2(body.getOpenTime2());
        shop.setCloseTime2(body.getCloseTime2());
        if (body.getWeightPricePer500g() != null) shop.setWeightPricePer500g(body.getWeightPricePer500g());
        if (body.getMinimumOrderWeightG() != null) shop.setMinimumOrderWeightG(Math.max(0, body.getMinimumOrderWeightG()));
        shopMapper.updateById(shop);
        auditService.record(adminId(),"UPDATE_SHOP","shop",String.valueOf(id),"修改商户资料与配置",request);
        return Result.success(null);
    }

    @Data public static class ReasonRequest { private String reason; }

    @PutMapping("/shops/{id}/reset-password")
    public Result<Map<String,String>> resetShopPassword(@PathVariable Long id,@RequestBody ReasonRequest body,HttpServletRequest request){
        if(isBlank(body.getReason()))return Result.error("请填写重置原因"); Shop shop=shopMapper.selectById(id);if(shop==null)return Result.error("商户不存在");
        String temporary=numericPassword(); shop.setPassword(PasswordUtil.encrypt(temporary));shop.setPwdUpdatedAt(null);shopMapper.updateById(shop);
        auditService.record(adminId(),"RESET_SHOP_PASSWORD","shop",String.valueOf(id),body.getReason(),request);
        return Result.success(Map.of("temporaryPassword",temporary));
    }

    @GetMapping("/canteens")
    public Result<List<Canteen>> canteens(){return Result.success(canteenMapper.selectList(new LambdaQueryWrapper<Canteen>().orderByAsc(Canteen::getSortOrder,Canteen::getId)));}
    @PostMapping("/canteens")
    public Result<Canteen> createCanteen(@RequestBody Canteen body,HttpServletRequest request){if(isBlank(body.getName()))return Result.error("食堂名称不能为空");canteenMapper.insert(body);auditService.record(adminId(),"CREATE_CANTEEN","canteen",String.valueOf(body.getId()),body.getName(),request);return Result.success(body);}
    @PutMapping("/canteens/{id}")
    public Result<Void> updateCanteen(@PathVariable Integer id,@RequestBody Canteen body,HttpServletRequest request){body.setId(id);canteenMapper.updateById(body);auditService.record(adminId(),"UPDATE_CANTEEN","canteen",String.valueOf(id),body.getName(),request);return Result.success(null);}

    @GetMapping("/users")
    public Result<Map<String,Object>> users(@RequestParam(defaultValue="1")int page,@RequestParam(defaultValue="20")int pageSize,@RequestParam(required=false)String keyword,@RequestParam(required=false)String penaltyStatus){
        List<Object> args=new ArrayList<>();StringBuilder where=new StringBuilder(" WHERE 1=1");
        if(!isBlank(keyword)){where.append(" AND (u.name LIKE ? OR u.phone LIKE ? OR CAST(u.id AS CHAR)=?)");String k="%"+keyword.trim()+"%";args.add(k);args.add(k);args.add(keyword.trim());}
        if(!isBlank(penaltyStatus)){where.append(" AND u.penalty_status=?");args.add(penaltyStatus);}
        Long total=jdbc.queryForObject("SELECT COUNT(*) FROM users u"+where,Long.class,args.toArray());List<Object> pa=new ArrayList<>(args);pa.add(safePageSize(pageSize));pa.add(offset(page,pageSize));
        List<Map<String,Object>> records=jdbc.queryForList("""
                SELECT u.id,u.phone,u.name,u.no_show_count,u.penalty_status,u.penalty_end_time,u.penalty_reason,u.create_time,
                COUNT(o.id) order_count,COALESCE(SUM(CASE WHEN o.status='completed' THEN o.total_amount ELSE 0 END),0) total_spent
                FROM users u LEFT JOIN orders o ON o.user_id=u.id
                """+where+" GROUP BY u.id ORDER BY u.create_time DESC LIMIT ? OFFSET ?",pa.toArray());return Result.success(page(records,total,page,pageSize));}

    @GetMapping("/users/summary")
    public Result<Map<String, Object>> userSummary() {
        Map<String, Object> result = new LinkedHashMap<>(jdbc.queryForMap("""
                SELECT COUNT(*) totalUsers,
                       SUM(create_time>=DATE_SUB(NOW(),INTERVAL 30 DAY)) newUsers30d,
                       SUM(penalty_status IS NOT NULL AND penalty_status<>'normal') restrictedUsers
                FROM users
                """));
        result.put("orderedUsers", jdbc.queryForObject("SELECT COUNT(DISTINCT user_id) FROM orders", Long.class));
        result.put("topUsers", jdbc.queryForList("""
                SELECT u.id,u.name,u.phone,COUNT(o.id) orderCount,
                       COALESCE(SUM(CASE WHEN o.status='completed' THEN o.total_amount ELSE 0 END),0) totalSpent
                FROM users u JOIN orders o ON o.user_id=u.id
                GROUP BY u.id,u.name,u.phone ORDER BY orderCount DESC,totalSpent DESC LIMIT 5
                """));
        return Result.success(result);
    }

    @GetMapping("/users/{id}/orders")
    public Result<List<Map<String,Object>>> userOrders(@PathVariable Long id){return Result.success(jdbc.queryForList("SELECT o.*,s.name shop_name FROM orders o JOIN shops s ON s.id=o.shop_id WHERE o.user_id=? ORDER BY o.create_time DESC LIMIT 100",id));}

    @Data public static class PenaltyRequest {private String status;private Integer noShowCount;private String reason;}
    @PutMapping("/users/{id}/penalty")
    public Result<Void> updatePenalty(@PathVariable Long id,@RequestBody PenaltyRequest body,HttpServletRequest request){
        Set<String> allowed=Set.of("normal","blocked_3d","blocked_7d","blocked_30d","frozen");if(!allowed.contains(body.getStatus())||isBlank(body.getReason()))return Result.error("处罚状态和原因不能为空");User user=userMapper.selectById(id);if(user==null)return Result.error("用户不存在");
        user.setPenaltyStatus(body.getStatus());user.setPenaltyReason("normal".equals(body.getStatus())?null:body.getReason());user.setNoShowCount(body.getNoShowCount()==null?user.getNoShowCount():Math.max(0,body.getNoShowCount()));
        LocalDateTime now=LocalDateTime.now();user.setPenaltyEndTime(switch(body.getStatus()){case "blocked_3d"->now.plusDays(3);case "blocked_7d"->now.plusDays(7);case "blocked_30d"->now.plusDays(30);default->null;});userMapper.updateById(user);
        auditService.record(adminId(),"UPDATE_USER_PENALTY","user",String.valueOf(id),body.getStatus()+"；"+body.getReason(),request);return Result.success(null);}

    @GetMapping("/feedbacks")
    public Result<Map<String,Object>> feedbacks(@RequestParam(defaultValue="1")int page,@RequestParam(defaultValue="20")int pageSize,@RequestParam(required=false)String status){
        String where=isBlank(status)?"":" WHERE f.status=?";Object[] base=isBlank(status)?new Object[]{}:new Object[]{status};Long total=jdbc.queryForObject("SELECT COUNT(*) FROM feedbacks f"+where,Long.class,base);
        List<Object> args=new ArrayList<>(Arrays.asList(base));args.add(safePageSize(pageSize));args.add(offset(page,pageSize));List<Map<String,Object>> records=jdbc.queryForList("SELECT f.*,u.name user_name,u.phone user_phone FROM feedbacks f LEFT JOIN users u ON u.id=f.user_id"+where+" ORDER BY f.create_time DESC LIMIT ? OFFSET ?",args.toArray());return Result.success(page(records,total,page,pageSize));}
    @PutMapping("/feedbacks/{id}")
    public Result<Void> updateFeedback(@PathVariable Long id,@RequestBody Feedback body,HttpServletRequest request){Feedback feedback=feedbackMapper.selectById(id);if(feedback==null)return Result.error("反馈不存在");feedback.setStatus(body.getStatus());feedback.setAdminReply(body.getAdminReply());feedback.setInternalNote(body.getInternalNote());feedback.setHandledAt("resolved".equals(body.getStatus())?LocalDateTime.now():null);feedbackMapper.updateById(feedback);auditService.record(adminId(),"HANDLE_FEEDBACK","feedback",String.valueOf(id),body.getStatus(),request);return Result.success(null);}

    @GetMapping("/config")
    public Result<Map<String,Object>> config(){Map<String,Object> data=new LinkedHashMap<>();data.put("reservationRule",reservationMapper.selectOne(new LambdaQueryWrapper<ReservationRuleConfig>().last("LIMIT 1")));data.put("luckyDraw",luckyDrawMapper.selectOne(new LambdaQueryWrapper<LuckyDrawConfig>().last("LIMIT 1")));return Result.success(data);}
    @PutMapping("/config/reservation")
    public Result<Void> updateReservation(@RequestBody ReservationRuleConfig body,HttpServletRequest request){if(body.getLunchPeakStart()==null||body.getLunchPeakEnd()==null||!body.getLunchPeakStart().isBefore(body.getLunchPeakEnd())||body.getDinnerPeakStart()==null||body.getDinnerPeakEnd()==null||!body.getDinnerPeakStart().isBefore(body.getDinnerPeakEnd()))return Result.error("高峰开始时间必须早于结束时间");ReservationRuleConfig existing=reservationMapper.selectOne(new LambdaQueryWrapper<ReservationRuleConfig>().last("LIMIT 1"));body.setId(existing==null?null:existing.getId());if(existing==null)reservationMapper.insert(body);else reservationMapper.updateById(body);auditService.record(adminId(),"UPDATE_RESERVATION_CONFIG","config",String.valueOf(body.getId()),"修改预约规则",request);return Result.success(null);}
    @PutMapping("/config/lucky-draw")
    public Result<Void> updateLuckyDraw(@RequestBody LuckyDrawConfig body,HttpServletRequest request){if(body.getReserveEndTime()==null||body.getDrawTime()==null||!body.getReserveEndTime().isBefore(body.getDrawTime()))return Result.error("预约截止时间必须早于开奖时间");LuckyDrawConfig existing=luckyDrawMapper.selectOne(new LambdaQueryWrapper<LuckyDrawConfig>().last("LIMIT 1"));body.setId(existing==null?null:existing.getId());if(existing==null)luckyDrawMapper.insert(body);else luckyDrawMapper.updateById(body);auditService.record(adminId(),"UPDATE_LUCKY_DRAW_CONFIG","config",String.valueOf(body.getId()),"修改抽奖配置",request);return Result.success(null);}

    @GetMapping("/audit-logs")
    public Result<List<AdminOperationLog>> auditLogs(){return Result.success(logMapper.selectList(new LambdaQueryWrapper<AdminOperationLog>().orderByDesc(AdminOperationLog::getCreateTime).last("LIMIT 100")));}

    @GetMapping("/export/{type}")
    public void export(@PathVariable String type,HttpServletResponse response)throws IOException{
        List<Map<String,Object>> rows;String[] headers;
        switch(type){
            case "orders"->{headers=new String[]{"id","shop_name","user_name","total_amount","status","order_mode","create_time"};rows=jdbc.queryForList("SELECT o.id,s.name shop_name,u.name user_name,o.total_amount,o.status,o.order_mode,o.create_time FROM orders o JOIN shops s ON s.id=o.shop_id LEFT JOIN users u ON u.id=o.user_id ORDER BY o.create_time DESC LIMIT 10000");}
            case "shops"->{headers=new String[]{"id","name","canteen_name","contact_phone","status","visible","shop_mode"};rows=jdbc.queryForList("SELECT s.id,s.name,c.name canteen_name,s.contact_phone,s.status,s.visible,s.shop_mode FROM shops s LEFT JOIN canteens c ON c.id=s.canteen_id ORDER BY s.display_sort,s.id");}
            case "users"->{headers=new String[]{"id","name","phone","no_show_count","penalty_status","create_time"};rows=jdbc.queryForList("SELECT id,name,phone,no_show_count,penalty_status,create_time FROM users ORDER BY create_time DESC LIMIT 10000");}
            case "feedbacks"->{headers=new String[]{"id","user_id","content","contact","status","admin_reply","create_time"};rows=jdbc.queryForList("SELECT id,user_id,content,contact,status,admin_reply,create_time FROM feedbacks ORDER BY create_time DESC LIMIT 10000");}
            default->{response.sendError(400,"Unsupported export type");return;}
        }
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());response.setContentType("text/csv;charset=UTF-8");response.setHeader("Content-Disposition","attachment; filename*=UTF-8''"+URLEncoder.encode(type+"-"+LocalDate.now()+".csv",StandardCharsets.UTF_8));
        StringBuilder csv=new StringBuilder("\uFEFF");csv.append(String.join(",",headers)).append("\r\n");for(Map<String,Object> row:rows){for(int i=0;i<headers.length;i++){if(i>0)csv.append(',');csv.append(csvValue(row.get(headers[i])));}csv.append("\r\n");}response.getWriter().write(csv.toString());
    }

    private SqlFilter orderFilter(String keyword,Long shopId,Long userId,String status,String orderMode,LocalDate startDate,LocalDate endDate){List<Object> args=new ArrayList<>();StringBuilder w=new StringBuilder(" WHERE 1=1");if(!isBlank(keyword)){w.append(" AND (o.id LIKE ? OR o.pickup_code LIKE ?)");String k="%"+keyword.trim()+"%";args.add(k);args.add(k);}if(shopId!=null){w.append(" AND o.shop_id=?");args.add(shopId);}if(userId!=null){w.append(" AND o.user_id=?");args.add(userId);}if(!isBlank(status)){w.append(" AND o.status=?");args.add(status);}if(!isBlank(orderMode)){w.append(" AND o.order_mode=?");args.add(orderMode);}if(startDate!=null){w.append(" AND o.create_time>=?");args.add(Timestamp.valueOf(startDate.atStartOfDay()));}if(endDate!=null){w.append(" AND o.create_time<?");args.add(Timestamp.valueOf(endDate.plusDays(1).atStartOfDay()));}return new SqlFilter(w.toString(),args);}
    private record SqlFilter(String where,List<Object> args){}
    private Map<String,Object> page(List<?> records,Long total,int page,int pageSize){return Map.of("records",records,"total",total==null?0:total,"page",Math.max(1,page),"pageSize",safePageSize(pageSize));}
    private int safePageSize(int size){return Math.min(100,Math.max(10,size));}private int offset(int page,int size){return(Math.max(1,page)-1)*safePageSize(size);}private Long adminId(){return(Long)SecurityContextHolder.getContext().getAuthentication().getPrincipal();}
    private boolean isBlank(String value){return value==null||value.trim().isEmpty();}private long asLong(Object value){return value==null?0:((Number)value).longValue();}private BigDecimal asDecimal(Object value){return value==null?BigDecimal.ZERO:new BigDecimal(value.toString());}
    private String csvValue(Object value){String text=value==null?"":String.valueOf(value);return"\""+text.replace("\"","\"\"")+"\"";}
    private String numericPassword(){return String.valueOf(100000+SECURE_RANDOM.nextInt(900000));}
}
