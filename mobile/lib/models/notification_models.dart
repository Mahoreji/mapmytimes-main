// =============================================================================
// Notification + Contact/Support form models — frontend/src/types/notification.ts
// =============================================================================

import 'blog_models.dart';

class NotificationItem {
  final ID id;
  final String? title;
  final String? body;
  final String? type;
  final String? status;
  final bool? read;
  final DateTime? createdAt;

  const NotificationItem({
    required this.id,
    this.title,
    this.body,
    this.type,
    this.status,
    this.read,
    this.createdAt,
  });

  factory NotificationItem.fromJson(Map<String, dynamic> j) => NotificationItem(
    id: (j['id'] ?? j['notificationId'] ?? '') as ID,
    title: (j['title'] ?? j['subject']) as String?,
    body: (j['body'] ?? j['message']) as String?,
    type: j['type'] as String?,
    status: j['status'] as String?,
    read: (j['read'] ?? j['isRead']) as bool?,
    createdAt: j['createdAt'] == null ? null : DateTime.tryParse(j['createdAt'].toString()),
  );
}

class UnreadCountResponse {
  final int unread;
  final int total;
  const UnreadCountResponse({this.unread = 0, this.total = 0});
  factory UnreadCountResponse.fromJson(Map<String, dynamic> j) => UnreadCountResponse(
    unread: (j['unread'] ?? j['unreadCount'] ?? 0) as int,
    total: (j['total'] ?? j['count'] ?? 0) as int,
  );
}

// =============================================================================
// Contact form (customer support / lead-capture)
// =============================================================================
class ContactFormRequest {
  final String name;
  final String email;
  final String? phone;
  final String subject;
  final String message;
  final String? source;
  final String? preferredLanguage;

  const ContactFormRequest({
    required this.name,
    required this.email,
    this.phone,
    required this.subject,
    required this.message,
    this.source = 'mapmytimes-mobile',
    this.preferredLanguage,
  });

  Map<String, dynamic> toJson() => <String, dynamic>{
    'name': name,
    'email': email,
    if (phone != null) 'phone': phone,
    'subject': subject,
    'message': message,
    if (source != null) 'source': source,
    if (preferredLanguage != null) 'preferredLanguage': preferredLanguage,
  };
}
