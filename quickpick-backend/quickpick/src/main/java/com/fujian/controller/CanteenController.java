package com.fujian.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fujian.common.Result;
import com.fujian.mapper.CanteenMapper;
import com.fujian.mapper.ShopMapper;
import com.fujian.pojo.Canteen;
import com.fujian.pojo.Shop;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/client/canteens")
public class CanteenController {

    @Autowired
    private CanteenMapper canteenMapper;
    
    @Autowired
    private ShopMapper shopMapper;

    @GetMapping
    public Result<List<Canteen>> getCanteens() {
        List<Canteen> canteens = canteenMapper.selectList(new LambdaQueryWrapper<Canteen>()
                .eq(Canteen::getStatus, 1)
                .orderByAsc(Canteen::getSortOrder));
                
        // Populate shopCount
        for (Canteen canteen : canteens) {
            Long visibleCount = shopMapper.selectCount(new LambdaQueryWrapper<Shop>()
                    .eq(Shop::getCanteenId, canteen.getId())
                    .eq(Shop::getVisible, 1));
            canteen.setShopCount(visibleCount);
        }
        
        return Result.success(canteens);
    }
}
