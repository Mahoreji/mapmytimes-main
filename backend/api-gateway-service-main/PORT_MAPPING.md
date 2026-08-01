# MapMyTour Service Port Mapping

---
### 🔧 **Services and Their Mapped Ports**

#### **Core Services (8080-8090)**
| Subdomain               | Service Name            | Port   | Status |
|-------------------------|-------------------------|--------|--------|
| `mapmytour.in`          | Frontend (Next.js)      | `3000` | ✅ Done |
| `api.mapmytour.in`      | API Gateway            | `8080` | ✅ Done |
| `auth.mapmytour.in`     | Auth Service            | `8081` | ✅ Done |
| `utils.mapmytour.in`    | Utils Service           | `8082` | ✅ Done |
| `core.mapmytour.in`     | Core Service            | `8083` | ✅ Done |
| `ai.mapmytour.in`       | AI Service              | `8084` | ✅ Done |
| `travel.mapmytour.in`   | Travel Service          | `8085` | ✅ Done |
| `customer.mapmytour.in` | Customer Support Service| `8086` | ✅ Done |
| `review.mapmytour.in`   | Reviews & Rating Service| `8087` | ✅ Done |
| `payment.mapmytour.in`  | Payment Service         | `8088` | ✅ Done |
| `booking.mapmytour.in`  | Booking Service         | `8089` | ✅ Done |
| `blog.mapmytour.in`     | Blog Service            | `8090` | ✅ Done |

#### **Newly Created Services (8092-8102)**
| Service Name                    | Port   | Description                          | Status |
|---------------------------------|--------|--------------------------------------|--------|
| `agent-service`                 | `8103` | Agent Portal, Wallet & Commission    | ✅ Done |
| `supplier-service`              | `8093` | Supplier Portal & Settlement         | ✅ Done |
| `employee-service`              | `8094` | HR Management                        | ✅ Done |
| `document-service`              | `8095` | File Management                      | ✅ Done |
| `accounting-gst-service`        | `8096` | Accounting & GST Management          | ✅ Done |
| `fraud-detection-service`       | `8097` | Fraud Detection & Security           | ✅ Done |
| `audit-log-service`             | `8098` | Activity Tracking & Audit Logs       | ✅ Done |
| `report-analytics-service`      | `8099` | Business Intelligence & Reports      | ✅ Done |
| `lead-service`                  | `8100` | Lead Management & Tracking           | ✅ Done |
| `loyalty-service`               | `8101` | Rewards & Points Management          | ✅ Done |
| `corporate-travel-service`      | `8102` | Corporate Travel Management          | ✅ Done |

#### **Additional Services**
| Service Name            | Port   | Description              | Status |
|-------------------------|--------|--------------------------|--------|
| `hotel-services`        | `8092` | Hotel Management         | ✅ Done |
| `notification-service`  | `9090` | Notification (Go)        | ✅ Done |
| `group-booking-service` | `8104` | Group Booking            | ✅ Done |

---

## Port Sequence Summary

All newly created services follow a sequential port numbering scheme starting from **8092**:

- **8092**: Hotel Service
- **8093**: Supplier Service
- **8094**: Employee Service
- **8095**: Document Service
- **8096**: Accounting & GST Service
- **8097**: Fraud Detection Service
- **8098**: Audit Log Service
- **8099**: Report & Analytics Service
- **8100**: Lead Service
- **8101**: Loyalty Service
- **8102**: Corporate Travel Service
- **8103**: Agent Service
- **9090**: Notification Service

---

# Service URLs Configuration

## Environment Variables for Service Discovery

```bash
# Core Services
AUTH_SERVICE_URL=http://auth.mapmytour.in
USER_SERVICE_URL=http://auth.mapmytour.in
PAYMENT_SERVICE_URL=http://payment.mapmytour.in
BOOKING_SERVICE_URL=http://booking.mapmytour.in
TRAVEL_SERVICE_URL=http://travel.mapmytour.in
REVIEWS_SERVICE_URL=http://review.mapmytour.in
BLOG_SERVICE_URL=http://blog.mapmytour.in
CUSTOMER_SUPPORT_SERVICE_URL=http://customer.mapmytour.in
UTILS_SERVICE_URL=http://utils.mapmytour.in
CORE_SERVICE_URL=http://core.mapmytour.in
CHAT_SERVICE_URL=http://chat.mapmytour.in

# Newly Created Services (Local Development)
AGENT_SERVICE_URL=http://localhost:8103
SUPPLIER_SERVICE_URL=http://localhost:8093
EMPLOYEE_SERVICE_URL=http://localhost:8094
DOCUMENT_SERVICE_URL=http://localhost:8095
GST_SERVICE_URL=http://localhost:8096
FRAUD_SERVICE_URL=http://localhost:8097
AUDIT_SERVICE_URL=http://localhost:8098
REPORT_SERVICE_URL=http://localhost:8099
LEAD_SERVICE_URL=http://localhost:8100
LOYALTY_SERVICE_URL=http://localhost:8101
CORPORATE_TRAVEL_SERVICE_URL=http://localhost:8102

# Production URLs (when deployed)
# AGENT_SERVICE_URL=https://agent.mapmytour.in
# SUPPLIER_SERVICE_URL=https://supplier.mapmytour.in
# EMPLOYEE_SERVICE_URL=https://employee.mapmytour.in
# DOCUMENT_SERVICE_URL=https://document.mapmytour.in
# GST_SERVICE_URL=https://gst.mapmytour.in
# FRAUD_SERVICE_URL=https://fraud.mapmytour.in
# AUDIT_SERVICE_URL=https://audit.mapmytour.in
# REPORT_SERVICE_URL=https://reports.mapmytour.in
# LEAD_SERVICE_URL=https://lead.mapmytour.in
# LOYALTY_SERVICE_URL=https://loyalty.mapmytour.in
# CORPORATE_TRAVEL_SERVICE_URL=https://corporate.mapmytour.in
```

## Notes

- All services are configured with production-ready credentials from `core-service`
- Database: PostgreSQL at `150.241.245.162:5432`
- Redis: `150.241.245.162:6379`
- AWS S3: Configured for file storage services
- JWT: Shared secret key across all services
- Eureka: Service discovery (disabled by default, can be enabled)

---
