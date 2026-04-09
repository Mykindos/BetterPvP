package me.mykindos.betterpvp.orchestration.model;

import java.util.Objects;

public record AdmissionDecision(
        AdmissionResult result,
        QueueTarget target,
        String reservationId,
        String ticketId,
        String reason
) {

    public AdmissionDecision {
        Objects.requireNonNull(result, "result");
        if (result != AdmissionResult.UNMANAGED) {
            Objects.requireNonNull(target, "target");
        }
    }

    public static AdmissionDecision unmanaged(String reason) {
        return new AdmissionDecision(AdmissionResult.UNMANAGED, null, null, null, reason);
    }

    public static AdmissionDecision bypass(QueueTarget target, String reason) {
        return new AdmissionDecision(AdmissionResult.BYPASS, target, null, null, reason);
    }

    public static AdmissionDecision granted(QueueTarget target, String reservationId, String reason) {
        return new AdmissionDecision(AdmissionResult.GRANTED, target, reservationId, null, reason);
    }

    public static AdmissionDecision queued(QueueTarget target, String ticketId, String reason) {
        return new AdmissionDecision(AdmissionResult.QUEUED, target, null, ticketId, reason);
    }

    public static AdmissionDecision denied(QueueTarget target, String reason) {
        return new AdmissionDecision(AdmissionResult.DENIED, target, null, null, reason);
    }
}
