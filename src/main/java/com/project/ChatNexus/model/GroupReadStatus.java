package com.project.ChatNexus.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

/**
 * Tracks when each user last read messages in a group.
 * Used to calculate unread message counts.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "group_read_status")
@CompoundIndex(def = "{'userId': 1, 'groupId': 1}", unique = true)
public class GroupReadStatus {
    @Id
    private String id;

    private String userId;
    private String groupId;
    private Date lastReadTimestamp;
}
