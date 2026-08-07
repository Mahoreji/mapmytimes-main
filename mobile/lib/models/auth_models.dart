// =============================================================================
// Auth service data models — mirror of frontend/src/types/auth.ts
// Uses envelope wrapper APIResponse<T> from blog_models.dart
// =============================================================================

import 'blog_models.dart';

// =============================================================================
// Auth response — { accessToken, refreshToken, tokenType, user }
// =============================================================================
class AuthResponse {
  final String accessToken;
  final String? refreshToken;
  final String? tokenType;
  final int? expiresIn;
  final UserResponse? user;
  final String? sessionId;

  const AuthResponse({
    required this.accessToken,
    this.refreshToken,
    this.tokenType = 'Bearer',
    this.expiresIn,
    this.user,
    this.sessionId,
  });

  factory AuthResponse.fromJson(Map<String, dynamic> j) => AuthResponse(
    accessToken: (j['accessToken'] ?? j['access_token'] ?? '') as String,
    refreshToken: (j['refreshToken'] ?? j['refresh_token']) as String?,
    tokenType: (j['tokenType'] ?? j['token_type'] ?? 'Bearer') as String?,
    expiresIn: (j['expiresIn'] ?? j['expires_in']) as int?,
    user: j['user'] == null ? null : UserResponse.fromJson(Map<String, dynamic>.from(j['user'] as Map)),
    sessionId: (j['sessionId'] ?? j['session_id']) as String?,
  );
}

// =============================================================================
// User profile / summary / detailed
// =============================================================================
class UserResponse {
  final ID id;
  final String email;
  final String? firstName;
  final String? lastName;
  final String? fullName;
  final String? phone;
  final String? avatarUrl;
  final String? status;
  final List<String>? roles;
  final bool? emailVerified;
  final DateTime? createdAt;

  const UserResponse({
    required this.id,
    required this.email,
    this.firstName,
    this.lastName,
    this.fullName,
    this.phone,
    this.avatarUrl,
    this.status,
    this.roles,
    this.emailVerified,
    this.createdAt,
  });

  String get displayName {
    if ((fullName ?? '').isNotEmpty) return fullName!;
    if ((firstName ?? '').isNotEmpty || (lastName ?? '').isNotEmpty) {
      return [firstName ?? '', lastName ?? ''].where((s) => s.isNotEmpty).join(' ');
    }
    return email.split('@').first;
  }

  factory UserResponse.fromJson(Map<String, dynamic> j) => UserResponse(
    id: (j['id'] ?? j['userId'] ?? '') as ID,
    email: (j['email'] ?? '') as String,
    firstName: (j['firstName'] ?? j['first_name']) as String?,
    lastName: (j['lastName'] ?? j['last_name']) as String?,
    fullName: (j['fullName'] ?? j['full_name'] ?? j['name']) as String?,
    phone: j['phone'] as String?,
    avatarUrl: (j['avatarUrl'] ?? j['avatar_url']) as String?,
    status: j['status'] as String?,
    roles: (j['roles'] as List<dynamic>?)?.map((e) => e.toString()).toList(growable: false),
    emailVerified: (j['emailVerified'] ?? j['email_verified']) as bool?,
    createdAt: j['createdAt'] == null ? null : DateTime.tryParse(j['createdAt'].toString()),
  );
}

// =============================================================================
// AuthRequest DTOs
// =============================================================================
class LoginRequest {
  final String email;
  final String password;
  final String? deviceInfo;
  const LoginRequest({required this.email, required this.password, this.deviceInfo});
  Map<String, dynamic> toJson() => <String, dynamic>{
    'email': email,
    'password': password,
    if (deviceInfo != null) 'deviceInfo': deviceInfo,
  };
}

class RegisterRequest {
  final String email;
  final String password;
  final String? firstName;
  final String? lastName;
  final String? phone;
  const RegisterRequest({
    required this.email,
    required this.password,
    this.firstName,
    this.lastName,
    this.phone,
  });
  Map<String, dynamic> toJson() => <String, dynamic>{
    'email': email,
    'password': password,
    if (firstName != null) 'firstName': firstName,
    if (lastName != null) 'lastName': lastName,
    if (phone != null) 'phone': phone,
  };
}

class VerifyEmailRequest {
  final String email;
  final String otp;
  const VerifyEmailRequest({required this.email, required this.otp});
  Map<String, dynamic> toJson() => {'email': email, 'otp': otp};
}

class ResendVerificationRequest {
  final String email;
  const ResendVerificationRequest(this.email);
  Map<String, dynamic> toJson() => {'email': email};
}

class SendOtpRequest {
  final String email;
  final String? phone;
  const SendOtpRequest({required this.email, this.phone});
  Map<String, dynamic> toJson() => <String, dynamic>{
    'email': email,
    if (phone != null) 'phone': phone,
  };
}

class LoginWithOtpRequest {
  final String email;
  final String otp;
  const LoginWithOtpRequest({required this.email, required this.otp});
  Map<String, dynamic> toJson() => {'email': email, 'otp': otp};
}

class RefreshTokenRequest {
  final String refreshToken;
  const RefreshTokenRequest(this.refreshToken);
  Map<String, dynamic> toJson() => {'refreshToken': refreshToken};
}

class ForgotPasswordStep1Request {
  final String email;
  const ForgotPasswordStep1Request(this.email);
  Map<String, dynamic> toJson() => {'email': email};
}

class ForgotPasswordStep2Request {
  final String email;
  final String otp;
  const ForgotPasswordStep2Request({required this.email, required this.otp});
  Map<String, dynamic> toJson() => {'email': email, 'otp': otp};
}

class ResetPasswordRequest {
  final String email;
  final String? otp;
  final String? resetToken;
  final String newPassword;
  const ResetPasswordRequest({
    required this.email,
    required this.newPassword,
    this.otp,
    this.resetToken,
  });
  Map<String, dynamic> toJson() => <String, dynamic>{
    'email': email,
    'newPassword': newPassword,
    if (otp != null) 'otp': otp,
    if (resetToken != null) 'resetToken': resetToken,
  };
}

class ChangePasswordRequest {
  final String currentPassword;
  final String newPassword;
  const ChangePasswordRequest({required this.currentPassword, required this.newPassword});
  Map<String, dynamic> toJson() => {
    'currentPassword': currentPassword,
    'newPassword': newPassword,
  };
}

class AccountStatusResponse {
  final String status;
  final bool? emailVerified;
  final bool? twoFactorEnabled;
  final String? message;
  const AccountStatusResponse({
    required this.status,
    this.emailVerified,
    this.twoFactorEnabled,
    this.message,
  });
  factory AccountStatusResponse.fromJson(Map<String, dynamic> j) => AccountStatusResponse(
    status: (j['status'] ?? 'UNKNOWN') as String,
    emailVerified: j['emailVerified'] as bool?,
    twoFactorEnabled: j['twoFactorEnabled'] as bool?,
    message: j['message'] as String?,
  );
}
