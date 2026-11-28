package com.easysport.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Cấu hình chính sách hủy, hoãn, trượt lịch và hoàn tiền vào Ví EasySport.
 * Các ngưỡng chưa được chủ dự án chốt để mặc định null để hệ thống tự đưa vào
 * trạng thái chờ Owner phê duyệt thay vì tự áp mức phạt hoặc tỷ lệ hoàn.
 */
@Component
@ConfigurationProperties(prefix = "easysport.policy")
public class BookingPolicyProperties {

    /**
     * Số giờ tối thiểu trước giờ bóng lăn để được coi là hủy sớm (mặc định 168 giờ = 7 ngày).
     */
    private int earlyCancelMinHours = 168;

    /**
     * Ngưỡng giờ sát trận (VD: 24 giờ). Null = chưa cấu hình -> đưa vào chờ Owner duyệt.
     */
    private Integer nearStartMaxHours;

    /**
     * Số lần khách chủ động hủy tối đa trong khoảng thời gian xét. Null = chưa cấu hình.
     */
    private Integer excessiveCancelLimit;

    /**
     * Khoảng thời gian (tính bằng ngày) dùng để đếm số lần khách chủ động hủy. Null = chưa cấu hình.
     */
    private Integer excessiveCancelWindowDays;

    /**
     * Tỷ lệ hoàn tiền (%) khi hủy muộn/sát giờ (0..100). Null = chưa cấu hình -> chờ Owner duyệt.
     */
    private Integer lateCancelRefundPercent;

    /**
     * Tỷ lệ hoàn tiền (%) khi vượt quá số lần chủ động hủy cho phép (0..100). Null = chưa cấu hình.
     */
    private Integer excessiveCancelRefundPercent;

    /**
     * Số tuần tối đa tìm lịch trượt kế tiếp (bao gồm sau buổi thứ 12).
     */
    private int rescheduleMaxWeeks = 16;

    public int getEarlyCancelMinHours() { return earlyCancelMinHours; }
    public void setEarlyCancelMinHours(int earlyCancelMinHours) { this.earlyCancelMinHours = earlyCancelMinHours; }

    public Integer getNearStartMaxHours() { return nearStartMaxHours; }
    public void setNearStartMaxHours(Integer nearStartMaxHours) { this.nearStartMaxHours = nearStartMaxHours; }

    public Integer getExcessiveCancelLimit() { return excessiveCancelLimit; }
    public void setExcessiveCancelLimit(Integer excessiveCancelLimit) { this.excessiveCancelLimit = excessiveCancelLimit; }

    public Integer getExcessiveCancelWindowDays() { return excessiveCancelWindowDays; }
    public void setExcessiveCancelWindowDays(Integer excessiveCancelWindowDays) { this.excessiveCancelWindowDays = excessiveCancelWindowDays; }

    public Integer getLateCancelRefundPercent() { return lateCancelRefundPercent; }
    public void setLateCancelRefundPercent(Integer lateCancelRefundPercent) { this.lateCancelRefundPercent = lateCancelRefundPercent; }

    public Integer getExcessiveCancelRefundPercent() { return excessiveCancelRefundPercent; }
    public void setExcessiveCancelRefundPercent(Integer excessiveCancelRefundPercent) { this.excessiveCancelRefundPercent = excessiveCancelRefundPercent; }

    public int getRescheduleMaxWeeks() { return rescheduleMaxWeeks; }
    public void setRescheduleMaxWeeks(int rescheduleMaxWeeks) { this.rescheduleMaxWeeks = rescheduleMaxWeeks; }

    public boolean isLateCancelPolicyConfigured() {
        return nearStartMaxHours != null && lateCancelRefundPercent != null;
    }

    public boolean isExcessiveCancelTrackingEnabled() {
        return excessiveCancelLimit != null && excessiveCancelWindowDays != null;
    }
}
