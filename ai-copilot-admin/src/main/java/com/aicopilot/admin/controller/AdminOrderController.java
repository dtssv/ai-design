package com.aicopilot.admin.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import lombok.RequiredArgsConstructor;
import com.aicopilot.admin.common.result.R;
import com.aicopilot.admin.entity.PaymentOrder;
import com.aicopilot.admin.mapper.PaymentOrderMapper;

/**
 * 管理端 - 订单管理
 */
@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class AdminOrderController {

    private final PaymentOrderMapper paymentOrderMapper;

    /** 获取订单列表 */
    @GetMapping
    public R<Map<String, Object>> list(@RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String orderNo) {
        LambdaQueryWrapper<PaymentOrder> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(status)) {
            wrapper.eq(PaymentOrder::getStatus, status);
        }
        if (StringUtils.hasText(orderNo)) {
            wrapper.like(PaymentOrder::getOrderNo, orderNo);
        }
        wrapper.orderByDesc(PaymentOrder::getCreatedAt);
        Page<PaymentOrder> result = paymentOrderMapper.selectPage(new Page<>(page, size), wrapper);

        Map<String, Object> data = new HashMap<>();
        data.put("total", result.getTotal());
        data.put("items", result.getRecords());
        return R.ok(data);
    }

    /** 获取订单详情 */
    @GetMapping("/{id}")
    public R<PaymentOrder> detail(@PathVariable Long id) {
        return R.ok(paymentOrderMapper.selectById(id));
    }

    /** 手动退款 */
    @PostMapping("/{id}/refund")
    public R<Void> refund(@PathVariable Long id) {
        PaymentOrder order = paymentOrderMapper.selectById(id);
        if (order == null) {
            return R.fail("订单不存在");
        }
        if (!"paid".equals(order.getStatus())) {
            return R.fail("仅已支付订单可退款");
        }
        order.setStatus("refunded");
        paymentOrderMapper.updateById(order);
        return R.ok(null, "退款成功");
    }
}