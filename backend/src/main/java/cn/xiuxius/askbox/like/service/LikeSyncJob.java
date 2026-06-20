package cn.xiuxius.askbox.like.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class LikeSyncJob {

    private final LikeService likeService;

    @Value("${askbox.likes.sync-batch-size:200}")
    private int batchSize;

    @Scheduled(fixedDelayString = "${askbox.likes.sync-interval-ms:5000}")
    public void sync() {
        likeService.syncDirtyCounts(batchSize);
    }
}
