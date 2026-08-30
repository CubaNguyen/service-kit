package com.servicekit.common.contract;

public interface IAuditable {
    Long getCreatedAt();
    void setCreatedAt(Long createdAt);

    Long getUpdatedAt();
    void setUpdatedAt(Long updatedAt);
}
