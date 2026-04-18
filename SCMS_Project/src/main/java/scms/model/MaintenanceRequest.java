package scms.model;

/**
 * Represents a maintenance issue reported by a user.
 */
public class MaintenanceRequest {
    public enum Status   { PENDING, ASSIGNED, COMPLETED }
    public enum Urgency  { LOW, MEDIUM, HIGH }

    private static int counter = 1;

    private final String requestId;
    private final String roomId;
    private final String reportedByUserId;
    private final String description;
    private final Urgency urgency;
    private Status status;
    private String assignedTo;   // userId of maintainer

    public MaintenanceRequest(String roomId, String reportedByUserId,
                              String description, Urgency urgency) {
        this.requestId         = "MR" + String.format("%04d", counter++);
        this.roomId            = roomId;
        this.reportedByUserId  = reportedByUserId;
        this.description       = description;
        this.urgency           = urgency;
        this.status            = Status.PENDING;
    }

    public String getRequestId()          { return requestId; }
    public String getRoomId()             { return roomId; }
    public String getReportedByUserId()   { return reportedByUserId; }
    public String getDescription()        { return description; }
    public Urgency getUrgency()           { return urgency; }
    public Status getStatus()             { return status; }
    public String getAssignedTo()         { return assignedTo; }

    public void assign(String userId) {
        this.assignedTo = userId;
        this.status     = Status.ASSIGNED;
    }

    public void complete() {
        this.status = Status.COMPLETED;
    }

    @Override
    public String toString() {
        return String.format("MR[%s] Room:%s | %s | Urgency:%s | Status:%s | AssignedTo:%s",
                requestId, roomId, description, urgency, status, assignedTo);
    }
}
