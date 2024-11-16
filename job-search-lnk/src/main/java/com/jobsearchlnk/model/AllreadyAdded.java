package com.jobsearchlnk.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;


@RedisHash
@RequiredArgsConstructor
@Getter
public class AllreadyAdded {
@Id
@NonNull String url;


}
