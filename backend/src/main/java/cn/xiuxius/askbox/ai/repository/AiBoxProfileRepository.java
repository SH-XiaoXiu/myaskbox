package cn.xiuxius.askbox.ai.repository;

import org.springframework.stereotype.Repository;

import cn.xiuxius.askbox.ai.entity.AiBoxProfileEntity;
import cn.xiuxius.askbox.ai.mapper.AiBoxProfileMapper;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class AiBoxProfileRepository {
    private final AiBoxProfileMapper mapper;

    public AiBoxProfileEntity findByBoxUserId(Long boxUserId) {
        return mapper.selectById(boxUserId);
    }

    public void upsert(AiBoxProfileEntity profile) {
        if (findByBoxUserId(profile.getBoxUserId()) == null) {
            mapper.insert(profile);
        } else {
            mapper.updateById(profile);
        }
    }
}
