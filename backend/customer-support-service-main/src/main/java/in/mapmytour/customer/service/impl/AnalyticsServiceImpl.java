package in.mapmytour.customer.service.impl;

import in.mapmytour.customer.dto.AgentPerformanceMetrics;
import in.mapmytour.customer.dto.AnalyticsResponse;
import in.mapmytour.customer.entity.SupportTicket;
import in.mapmytour.customer.repository.CustomerFeedbackRepository;
import in.mapmytour.customer.repository.CustomerSupportAgentRepository;
import in.mapmytour.customer.repository.SupportTicketRepository;
import in.mapmytour.customer.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsServiceImpl implements AnalyticsService {

    private final SupportTicketRepository ticketRepository;
    private final CustomerSupportAgentRepository agentRepository;
    private final CustomerFeedbackRepository feedbackRepository;

    @Override
    @Transactional(readOnly = true)
    public AnalyticsResponse getAnalytics(LocalDateTime startDate, LocalDateTime endDate) {
        log.debug("Fetching analytics for period: {} to {}", startDate, endDate);
        
        List<SupportTicket> tickets = ticketRepository.findByCreatedAtBetween(startDate, endDate);
        
        int totalTickets = tickets.size();
        long openTickets = tickets.stream().filter(t -> t.getStatus() == SupportTicket.TicketStatus.OPEN).count();
        long inProgressTickets = tickets.stream().filter(t -> t.getStatus() == SupportTicket.TicketStatus.IN_PROGRESS).count();
        long resolvedTickets = tickets.stream().filter(t -> t.getStatus() == SupportTicket.TicketStatus.RESOLVED).count();
        long closedTickets = tickets.stream().filter(t -> t.getStatus() == SupportTicket.TicketStatus.CLOSED).count();
        
        // Calculate average response and resolution times
        double avgResponseTime = tickets.stream()
                .filter(t -> t.getResponseTimeMinutes() != null)
                .mapToLong(SupportTicket::getResponseTimeMinutes)
                .average()
                .orElse(0.0);
        
        double avgResolutionTime = tickets.stream()
                .filter(t -> t.getResolutionTimeMinutes() != null)
                .mapToLong(SupportTicket::getResolutionTimeMinutes)
                .average()
                .orElse(0.0);
        
        return AnalyticsResponse.builder()
                .totalTickets(totalTickets)
                .openTickets((int) openTickets)
                .inProgressTickets((int) inProgressTickets)
                .resolvedTickets((int) resolvedTickets)
                .closedTickets((int) closedTickets)
                .averageResponseTimeMinutes(avgResponseTime)
                .averageResolutionTimeMinutes(avgResolutionTime)
                .slaMetrics(getSLAMetrics(startDate, endDate))
                .ticketVolumeTrends(getTicketVolumeTrends(startDate, endDate))
                .categoryDistribution(getCategoryDistribution(startDate, endDate))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AgentPerformanceMetrics> getAgentPerformanceMetrics(LocalDateTime startDate, LocalDateTime endDate) {
        log.debug("Fetching agent performance metrics");
        
        List<AgentPerformanceMetrics> metrics = new ArrayList<>();
        var agents = agentRepository.findAll();
        
        for (var agent : agents) {
            List<SupportTicket> agentTickets = ticketRepository.findByCreatedAtBetween(startDate, endDate)
                    .stream()
                    .filter(t -> agent.getId().equals(t.getAssignedAgentId()))
                    .toList();
            
            if (agentTickets.isEmpty()) {
                continue;
            }
            
            int totalAssigned = agentTickets.size();
            long resolved = agentTickets.stream()
                    .filter(t -> t.getStatus() == SupportTicket.TicketStatus.RESOLVED || 
                            t.getStatus() == SupportTicket.TicketStatus.CLOSED)
                    .count();
            long open = agentTickets.stream()
                    .filter(t -> t.getStatus() == SupportTicket.TicketStatus.OPEN || 
                            t.getStatus() == SupportTicket.TicketStatus.IN_PROGRESS)
                    .count();
            
            double avgResponseTime = agentTickets.stream()
                    .filter(t -> t.getResponseTimeMinutes() != null)
                    .mapToLong(SupportTicket::getResponseTimeMinutes)
                    .average()
                    .orElse(0.0);
            
            double avgResolutionTime = agentTickets.stream()
                    .filter(t -> t.getResolutionTimeMinutes() != null)
                    .mapToLong(SupportTicket::getResolutionTimeMinutes)
                    .average()
                    .orElse(0.0);
            
            long slaResponseMet = agentTickets.stream()
                    .filter(t -> Boolean.TRUE.equals(t.getSlaResponseMet()))
                    .count();
            long slaResponseBreached = agentTickets.stream()
                    .filter(t -> Boolean.FALSE.equals(t.getSlaResponseMet()) && t.getSlaResponseMet() != null)
                    .count();
            long slaResolutionMet = agentTickets.stream()
                    .filter(t -> Boolean.TRUE.equals(t.getSlaResolutionMet()))
                    .count();
            long slaResolutionBreached = agentTickets.stream()
                    .filter(t -> Boolean.FALSE.equals(t.getSlaResolutionMet()) && t.getSlaResolutionMet() != null)
                    .count();
            
            // Get customer satisfaction for agent
            List<String> ticketIds = agentTickets.stream()
                    .map(SupportTicket::getId)
                    .toList();
            
            double avgRating = 0.0;
            long totalFeedbacks = 0;
            if (!ticketIds.isEmpty()) {
                Double dbAvgRating = feedbackRepository.findAverageRatingByTicketIdIn(ticketIds);
                if (dbAvgRating != null) {
                    avgRating = dbAvgRating;
                }
                Long dbCount = feedbackRepository.countByTicketIdIn(ticketIds);
                if (dbCount != null) {
                    totalFeedbacks = dbCount;
                }
            }
            
            metrics.add(AgentPerformanceMetrics.builder()
                    .agentId(agent.getId())
                    .agentName(agent.getFullName())
                    .agentEmail(agent.getEmail())
                    .totalTicketsAssigned(totalAssigned)
                    .ticketsResolved((int) resolved)
                    .ticketsOpen((int) open)
                    .averageResponseTimeMinutes(avgResponseTime)
                    .averageResolutionTimeMinutes(avgResolutionTime)
                    .slaResponseMet((int) slaResponseMet)
                    .slaResponseBreached((int) slaResponseBreached)
                    .slaResolutionMet((int) slaResolutionMet)
                    .slaResolutionBreached((int) slaResolutionBreached)
                    .customerSatisfactionRating(avgRating)
                    .totalFeedbacks((int) totalFeedbacks)
                    .build());
        }
        
        return metrics;
    }

    @Override
    @Transactional(readOnly = true)
    public AnalyticsResponse.SLAMetrics getSLAMetrics(LocalDateTime startDate, LocalDateTime endDate) {
        List<SupportTicket> tickets = ticketRepository.findByCreatedAtBetween(startDate, endDate);
        
        int total = tickets.size();
        long responseMet = tickets.stream()
                .filter(t -> Boolean.TRUE.equals(t.getSlaResponseMet()))
                .count();
        long responseBreached = tickets.stream()
                .filter(t -> Boolean.FALSE.equals(t.getSlaResponseMet()) && t.getSlaResponseMet() != null)
                .count();
        long resolutionMet = tickets.stream()
                .filter(t -> Boolean.TRUE.equals(t.getSlaResolutionMet()))
                .count();
        long resolutionBreached = tickets.stream()
                .filter(t -> Boolean.FALSE.equals(t.getSlaResolutionMet()) && t.getSlaResolutionMet() != null)
                .count();
        
        double responseSLAPercentage = total > 0 ? (responseMet * 100.0) / total : 0.0;
        double resolutionSLAPercentage = total > 0 ? (resolutionMet * 100.0) / total : 0.0;
        
        return AnalyticsResponse.SLAMetrics.builder()
                .totalTickets(total)
                .responseSLAMet((int) responseMet)
                .responseSLABreached((int) responseBreached)
                .resolutionSLAMet((int) resolutionMet)
                .resolutionSLABreached((int) resolutionBreached)
                .responseSLAPercentage(responseSLAPercentage)
                .resolutionSLAPercentage(resolutionSLAPercentage)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AnalyticsResponse.TicketVolumeTrend> getTicketVolumeTrends(LocalDateTime startDate, LocalDateTime endDate) {
        List<SupportTicket> tickets = ticketRepository.findByCreatedAtBetween(startDate, endDate);
        
        Map<String, Long> ticketsByDate = tickets.stream()
                .collect(Collectors.groupingBy(
                        t -> t.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE),
                        Collectors.counting()));
        
        Map<String, Long> resolvedByDate = tickets.stream()
                .filter(t -> t.getStatus() == SupportTicket.TicketStatus.RESOLVED || 
                        t.getStatus() == SupportTicket.TicketStatus.CLOSED)
                .collect(Collectors.groupingBy(
                        t -> t.getResolvedAt() != null ? 
                                t.getResolvedAt().format(DateTimeFormatter.ISO_LOCAL_DATE) :
                                t.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE),
                        Collectors.counting()));
        
        return ticketsByDate.entrySet().stream()
                .map(entry -> AnalyticsResponse.TicketVolumeTrend.builder()
                        .date(entry.getKey())
                        .ticketCount(entry.getValue().intValue())
                        .resolvedCount(resolvedByDate.getOrDefault(entry.getKey(), 0L).intValue())
                        .build())
                .sorted((a, b) -> a.getDate().compareTo(b.getDate()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AnalyticsResponse.CategoryDistribution> getCategoryDistribution(LocalDateTime startDate, LocalDateTime endDate) {
        List<SupportTicket> tickets = ticketRepository.findByCreatedAtBetween(startDate, endDate);
        
        int total = tickets.size();
        
        Map<SupportTicket.TicketCategory, Long> categoryCounts = tickets.stream()
                .collect(Collectors.groupingBy(
                        SupportTicket::getCategory,
                        Collectors.counting()));
        
        return categoryCounts.entrySet().stream()
                .map(entry -> AnalyticsResponse.CategoryDistribution.builder()
                        .category(entry.getKey().name())
                        .count(entry.getValue().intValue())
                        .percentage(total > 0 ? (entry.getValue() * 100.0) / total : 0.0)
                        .build())
                .sorted((a, b) -> Integer.compare(b.getCount(), a.getCount()))
                .toList();
    }
}

