package interswitch.academy.verve_guard.models.enums;

public enum KycStatus {
    PENDING,       // submitted, awaiting review
    IN_REVIEW,     // being actively reviewed
    APPROVED,      // verified, full access
    REJECTED,      // failed verification, can resubmit
    SUSPENDED      // was approved, now under review again
}
