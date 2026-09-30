package com.movie.api.model.vo;

import com.movie.api.model.entity.Worker;
import lombok.Getter;

/** Fields displayed on the public customer-service review cards. */
@Getter
public final class WorkerSummary {
    private final String id;
    private final String nickname;
    private final String gender;
    private final String avatar;

    public WorkerSummary(Worker worker) {
        this.id = worker.getId();
        this.nickname = worker.getNickname();
        this.gender = worker.getGender();
        this.avatar = worker.getAvatar();
    }
}
