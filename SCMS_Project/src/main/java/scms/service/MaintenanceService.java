package scms.service;

import scms.exception.EntityNotFoundException;
import scms.model.MaintenanceRequest;
import scms.model.MaintenanceRequest.Urgency;
import scms.model.User;
import scms.pattern.NotificationService;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Manages maintenance requests: reporting, assignment, and completion.
 * Fires Observer notifications on every status transition.
 */
public class MaintenanceService {

    private final Map<String, MaintenanceRequest> requests = new LinkedHashMap<>();
    private final RoomService roomService;

    public MaintenanceService(RoomService roomService) {
        this.roomService = roomService;
    }

    public MaintenanceRequest report(User reporter, String roomId,
                                     String description, Urgency urgency)
            throws EntityNotFoundException {

        // Validate room exists
        roomService.findById(roomId);

        MaintenanceRequest req = new MaintenanceRequest(
                roomId, reporter.getUserId(), description, urgency);
        requests.put(req.getRequestId(), req);

        NotificationService.getInstance().notify(
                reporter.getUserId(),
                "Maintenance request " + req.getRequestId()
                        + " submitted for Room " + roomId + ". Status: PENDING"
        );

        return req;
    }

    public void assign(String requestId, String assigneeUserId)
            throws EntityNotFoundException {

        MaintenanceRequest req = findById(requestId);
        req.assign(assigneeUserId);

        // Notify the original reporter
        NotificationService.getInstance().notify(
                req.getReportedByUserId(),
                "Maintenance request " + requestId + " has been ASSIGNED to " + assigneeUserId
        );
        // Notify the assignee
        NotificationService.getInstance().notify(
                assigneeUserId,
                "You have been assigned maintenance request " + requestId
                        + " (Room: " + req.getRoomId() + "): " + req.getDescription()
        );
    }

    public void complete(String requestId) throws EntityNotFoundException {
        MaintenanceRequest req = findById(requestId);
        req.complete();

        NotificationService.getInstance().notify(
                req.getReportedByUserId(),
                "Maintenance request " + requestId + " is now COMPLETED. Thank you for reporting!"
        );
    }

    public MaintenanceRequest findById(String requestId) throws EntityNotFoundException {
        MaintenanceRequest r = requests.get(requestId);
        if (r == null) throw new EntityNotFoundException("Maintenance request not found: " + requestId);
        return r;
    }

    public List<MaintenanceRequest> getAllRequests() {
        return new ArrayList<>(requests.values());
    }

    public List<MaintenanceRequest> getRequestsByUser(String userId) {
        return requests.values().stream()
                .filter(r -> r.getReportedByUserId().equals(userId))
                .collect(Collectors.toList());
    }

    /** Analytics helper */
    public void printMaintenanceStats() {
        Map<MaintenanceRequest.Status, Long> byStatus = requests.values().stream()
                .collect(Collectors.groupingBy(MaintenanceRequest::getStatus, Collectors.counting()));

        System.out.println("-- Maintenance Stats --");
        byStatus.forEach((s, c) -> System.out.printf("  %-10s : %d request(s)%n", s, c));

        if (requests.isEmpty()) System.out.println("  No maintenance requests yet.");
    }
}
