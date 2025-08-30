package dev.srivatsan.kubeapi.model;

import lombok.Data;

import java.util.List;

@Data
public class PodDetails {
    private String name;
    private String ip;
    private boolean isRunning;
    private List<FailureReason> failureReason;
}
