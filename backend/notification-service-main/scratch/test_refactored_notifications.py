import sys
import time
import requests

BASE_URL = "http://localhost:9090/api/v1"
recipient = "test_user_refactor_123@example.com"

def run_tests():
    print("🚀 Starting End-to-End Notification API Refactor Tests...\n")
    
    # 1. Fetch initial unread count
    print("--- 1. Get Unread Count (Initial) ---")
    url = f"{BASE_URL}/notifications/unread-count?recipient={recipient}"
    r = requests.get(url)
    assert r.status_code == 200, f"Unread count query failed: {r.text}"
    initial_unread = r.json()["data"]["unreadCount"]
    print(f"✓ Initial unread count retrieved successfully: {initial_unread}\n")

    # 2. Trigger Instant PAYMENT_SUCCESS Notification
    print("--- 2. Create Instant PAYMENT_SUCCESS Notification ---")
    payload = {
        "type": "PAYMENT_SUCCESS",
        "recipient": recipient,
        "subject": "Payment Receipt",
        "body": "Payment Processing", # Passes required binding validation
        "metadata": {
            "bookingId": "mapmytour-123",
            "bookingType": "FLIGHT",
            "serviceName": "Mumbai → Dubai Flight",
            "source": "Mumbai",
            "destination": "Dubai",
            "travelDate": "2026-06-15",
            "paymentId": "pay_123",
            "amount": "22893.80",
            "currency": "₹",
            "paymentMethod": "UPI",
            "paymentStatus": "SUCCESS"
        }
    }
    url = f"{BASE_URL}/notification/send/instant"
    r = requests.post(url, json=payload)
    assert r.status_code == 200, f"Instant notification trigger failed: {r.text}"
    notif_id = r.json()["data"]["id"]
    print(f"✓ Notification created and queued successfully: ID={notif_id}")
    
    # Wait a moment for background worker
    time.sleep(1.5)
    print()

    # 3. Retrieve and Validate Enriched Paginated History
    print("--- 3. Retrieve and Validate Standardized History DTO ---")
    url = f"{BASE_URL}/notifications?recipient={recipient}&page=1&limit=10"
    r = requests.get(url)
    assert r.status_code == 200, f"History fetch failed: {r.text}"
    history_resp = r.json()
    
    # Validate envelope
    assert history_resp["success"] is True
    assert history_resp["statusCode"] == 200
    
    data = history_resp["data"]["notifications"]
    pagination = history_resp["data"]["pagination"]
    counts = history_resp["data"]["counts"]
    print(f"  Counts -> Total: {counts['total']}, Unread: {counts['unread']}, Read: {counts['read']}")
    
    # Check pagination block
    print(f"  Pagination total: {pagination['total']}, page: {pagination['page']}, totalPages: {pagination['totalPages']}")
    assert pagination["page"] == 1
    assert pagination["limit"] == 10
    assert pagination["total"] > 0
    
    # Find our notification
    target_notif = None
    for n in data:
        if n["notificationId"] == notif_id:
            target_notif = n
            break
            
    assert target_notif is not None, "Created notification was not found in history"
    print("✓ Notification found in history list")

    # Validate DTO payload structure
    print("  Validating Rich Response DTO properties...")
    assert target_notif["type"] == "PAYMENT"
    assert target_notif["eventType"] == "PAYMENT_SUCCESS"
    assert target_notif["category"] == "PAYMENT"
    assert target_notif["priority"] == "HIGH"
    assert target_notif["title"] == "Payment Successful"
    assert target_notif["message"] == "Payment of ₹22893.80 received successfully."
    assert target_notif["isRead"] is False
    
    # Validate Booking Enrichment
    booking = target_notif["booking"]
    print(f"  Booking Summary -> ID: {booking['bookingId']}, Service: {booking['serviceName']}, Type: {booking['bookingType']}")
    assert booking["bookingId"] == "mapmytour-123"
    assert booking["bookingType"] == "FLIGHT"
    assert booking["serviceName"] == "Mumbai → Dubai Flight"
    assert booking["from"] == "Mumbai"
    assert booking["to"] == "Dubai"
    assert booking["destination"] == "Dubai"
    assert booking["travelDate"] == "2026-06-15"
    
    # Validate Payment Enrichment
    payment = target_notif["payment"]
    print(f"  Payment Summary -> ID: {payment['paymentId']}, Amount: {payment['amount']}, Method: {payment['paymentMethod']}")
    assert payment["paymentId"] == "pay_123"
    assert payment["amount"] == 22893.80
    assert payment["currency"] == "₹"
    assert payment["paymentStatus"] == "SUCCESS"
    
    # Validate Actions System
    action = target_notif["action"]
    print(f"  Action -> {action}")
    assert action["label"] == "View Details"
    assert action["type"] == "INTERNAL"
    assert action["url"] == "/bookings/mapmytour-123"
    
    # Validate UI Branding Metadata
    ui = target_notif["ui"]
    print(f"  UI -> Brand Icon: {ui['icon']}, Color: {ui['color']}, Deeplink: {ui['deeplink']}")
    assert ui["icon"] == "payment-success"
    assert ui["color"] == "green"
    assert ui["badge"] == "Success"
    assert ui["deeplink"] == "/bookings/mapmytour-123"
    print("✓ DTO properties validated perfectly.\n")

    # 4. Mark specific notification as Read
    print("--- 4. Mark Notification as Read ---")
    url = f"{BASE_URL}/notifications/{notif_id}/read"
    r = requests.patch(url)
    assert r.status_code == 200, f"Mark read failed: {r.text}"
    print("✓ Marked as read successfully")
    
    # Fetch history again and verify read status is updated
    url = f"{BASE_URL}/notifications?recipient={recipient}&page=1&limit=10"
    r = requests.get(url)
    updated_notif = next(n for n in r.json()["data"]["notifications"] if n["notificationId"] == notif_id)
    assert updated_notif["isRead"] is True, "Read field was not updated to true"
    assert updated_notif["readAt"] is not None, "ReadAt timestamp was not set"
    print("✓ Read and ReadAt verified successfully in history DTO.\n")

    # 5. Create another notification and perform Mark-All-As-Read
    print("--- 5. Trigger Second Notification and Mark All as Read ---")
    payload["type"] = "BOOKING_CONFIRMED"
    payload["metadata"]["serviceName"] = "Dubai Desert Safari"
    payload["metadata"]["bookingType"] = "TOUR"
    url = f"{BASE_URL}/notification/send/instant"
    r = requests.post(url, json=payload)
    new_notif_id = r.json()["data"]["id"]
    
    time.sleep(1.0)
    
    # Fetch unread count, should be > 0
    url = f"{BASE_URL}/notifications/unread-count?recipient={recipient}"
    unread_count = requests.get(url).json()["data"]["unreadCount"]
    assert unread_count >= 1, "Unread count should be at least 1"
    print(f"  Unread count is {unread_count} before read-all call")
    
    # Trigger Mark All as Read
    url = f"{BASE_URL}/notifications/read-all?recipient={recipient}"
    r = requests.patch(url)
    assert r.status_code == 200, f"Read-all failed: {r.text}"
    print("✓ Mark all as read completed successfully")
    
    # Fetch unread count, should be 0
    url = f"{BASE_URL}/notifications/unread-count?recipient={recipient}"
    unread_count = requests.get(url).json()["data"]["unreadCount"]
    assert unread_count == 0, f"Unread count should be 0, but is {unread_count}"
    print("✓ Unread count is verified to be 0 after read-all call.\n")

    # 6. Delete specific notification by ID
    print("--- 6. Delete Specific Notification ---")
    url = f"{BASE_URL}/notifications/{new_notif_id}"
    r = requests.delete(url)
    assert r.status_code == 200, f"Delete specific notification failed: {r.text}"
    print("✓ Specific notification deleted successfully")
    
    # Verify it is no longer in history
    url = f"{BASE_URL}/notifications?recipient={recipient}&page=1&limit=10"
    r = requests.get(url)
    history_data = r.json()["data"]["notifications"]
    deleted_found = any(n["notificationId"] == new_notif_id for n in history_data)
    assert not deleted_found, "Notification was not deleted from database"
    print("✓ Verified notification is gone from history\n")

    # 6A. Hotel Booking Notification Flow (Fix 9)
    print("--- 6A. Hotel Booking Notification Flow ---")
    hotel_payload = {
        "type": "BOOKING_CONFIRMED",
        "recipient": recipient,
        "subject": "Hotel Booking Confirmed",
        "body": "Your Taj Hotel booking has been confirmed.",
        "metadata": {
            "bookingId": "BK-HOTEL-778",
            "bookingType": "HOTEL",
            "serviceName": "Taj Hotel",
            "hotelName": "Taj Hotel",
            "checkInDate": "2026-06-15",
            "checkOutDate": "2026-06-20",
            "roomCount": "2",
            "bookingStatus": "CONFIRMED",
            "totalAmount": "15000.00",
            "currency": "INR"
        }
    }
    url = f"{BASE_URL}/notification/send/instant"
    r = requests.post(url, json=hotel_payload)
    assert r.status_code == 200, f"Hotel notification failed: {r.text}"
    hotel_notif_id = r.json()["data"]["id"]
    print(f"✓ Hotel notification created successfully: ID={hotel_notif_id}")

    # Fetch and verify in GET response
    url = f"{BASE_URL}/notifications?recipient={recipient}&page=1&limit=10"
    r = requests.get(url)
    history_data = r.json()["data"]["notifications"]
    hotel_notif = next(n for n in history_data if n["notificationId"] == hotel_notif_id)
    assert hotel_notif["category"] == "BOOKING"
    assert hotel_notif["type"] == "HOTEL"
    assert hotel_notif["booking"]["bookingId"] == "BK-HOTEL-778"
    assert hotel_notif["booking"]["hotelName"] == "Taj Hotel"
    assert hotel_notif["booking"]["roomCount"] == 2
    print("✓ Verified Hotel booking notification fields successfully\n")

    # 6B. Flight Booking Notification Flow (Fix 9)
    print("--- 6B. Flight Booking Notification Flow ---")
    flight_payload = {
        "type": "BOOKING_CONFIRMED",
        "recipient": recipient,
        "subject": "Flight Ticket Confirmed",
        "body": "Your Flight EK-501 has been confirmed.",
        "metadata": {
            "bookingId": "BK-FLIGHT-992",
            "bookingType": "FLIGHT",
            "serviceName": "Emirates Flight",
            "airline": "Emirates",
            "flightNumber": "EK-501",
            "from": "Mumbai",
            "to": "Dubai",
            "bookingStatus": "CONFIRMED",
            "travelDate": "2026-06-15"
        }
    }
    url = f"{BASE_URL}/notification/send/instant"
    r = requests.post(url, json=flight_payload)
    assert r.status_code == 200, f"Flight notification failed: {r.text}"
    flight_notif_id = r.json()["data"]["id"]
    print(f"✓ Flight notification created successfully: ID={flight_notif_id}")

    # Fetch and verify in GET response
    url = f"{BASE_URL}/notifications?recipient={recipient}&page=1&limit=10"
    r = requests.get(url)
    history_data = r.json()["data"]["notifications"]
    flight_notif = next(n for n in history_data if n["notificationId"] == flight_notif_id)
    assert flight_notif["category"] == "BOOKING"
    assert flight_notif["type"] == "FLIGHT"
    assert flight_notif["booking"]["bookingId"] == "BK-FLIGHT-992"
    assert flight_notif["booking"]["from"] == "Mumbai"
    assert flight_notif["booking"]["to"] == "Dubai"
    print("✓ Verified Flight booking notification fields successfully\n")

    # 6C. Delete Specific Booking Notification (Fix 9)
    print("--- 6C. Delete Specific Booking Notification ---")
    url = f"{BASE_URL}/notifications/{hotel_notif_id}"
    r = requests.delete(url)
    assert r.status_code == 200, f"Delete hotel notification failed: {r.text}"
    print("✓ Hotel booking notification deleted successfully")

    # Verify it is no longer in history
    url = f"{BASE_URL}/notifications?recipient={recipient}&page=1&limit=10"
    r = requests.get(url)
    history_data = r.json()["data"]["notifications"]
    hotel_found = any(n["notificationId"] == hotel_notif_id for n in history_data)
    assert not hotel_found, "Hotel notification was not deleted from database"
    print("✓ Verified hotel notification is gone from history\n")

    # 7. Delete all notifications for recipient
    print("--- 7. Delete All Notifications for Recipient ---")
    url = f"{BASE_URL}/notifications?recipient={recipient}"
    r = requests.delete(url)
    assert r.status_code == 200, f"Delete all notifications failed: {r.text}"
    print("✓ All notifications deleted successfully")
    
    # Verify history is now empty (or at least none for our test recipient)
    url = f"{BASE_URL}/notifications?recipient={recipient}&page=1&limit=10"
    r = requests.get(url)
    resp_json = r.json()
    history_data = resp_json["data"]["notifications"]
    assert len(history_data) == 0, f"History should be empty for recipient, but got: {history_data}"
    assert resp_json["data"]["counts"]["total"] == 0, "Counts total should be 0 after delete all"
    assert resp_json["data"]["counts"]["unread"] == 0, "Counts unread should be 0 after delete all"
    assert resp_json["data"]["counts"]["read"] == 0, "Counts read should be 0 after delete all"
    assert resp_json["data"]["pagination"]["total"] == 0, "Pagination total should be 0 after delete all"
    print("✓ Verified counts = 0 and pagination total = 0 after delete-all\n")

    print("🎉 ALL TESTS PASSED SUCCESSFULLY! The Notification API refactoring matches the requirements 100%.")

if __name__ == "__main__":
    try:
        run_tests()
    except AssertionError as e:
        print(f"\n❌ TEST FAILURE: {e}")
        sys.exit(1)
    except Exception as e:
        print(f"\n❌ UNEXPECTED ERROR: {e}")
        sys.exit(1)
