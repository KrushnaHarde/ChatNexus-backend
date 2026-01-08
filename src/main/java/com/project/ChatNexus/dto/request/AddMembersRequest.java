package com.project.ChatNexus.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.*;

import java.util.Set;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddMembersRequest {

    @NotEmpty(message = "At least one member must be added")
    private Set<String> memberIds;
}
