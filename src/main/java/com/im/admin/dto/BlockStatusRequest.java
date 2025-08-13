package com.im.admin.dto;

import lombok.Data;

@Data
public class BlockStatusRequest {
    private boolean block; // true = block(1), false = unblock(0)
}
