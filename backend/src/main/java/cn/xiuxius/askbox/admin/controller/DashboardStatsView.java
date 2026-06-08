package cn.xiuxius.askbox.admin.controller;

/**
 * 仪表盘统计数据视图。
 */
public record DashboardStatsView(long userCount, long boxCount, long questionCount, long attachmentCount) {}
