package cn.xiuxius.askbox.ai.repository;

import org.springframework.stereotype.Repository;

import cn.xiuxius.askbox.ai.entity.AiCallLogEntity;
import cn.xiuxius.askbox.ai.mapper.AiCallLogMapper;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class AiCallLogRepository {
    private final AiCallLogMapper mapper;

    public void insert(AiCallLogEntity log) {
        mapper.insert(log);
    }
}
