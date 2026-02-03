package com.sekolah.aplikasismpn3pacet.data

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Date

class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }

    @TypeConverter
    fun fromStringList(value: String?): List<String>? {
        if (value == null) return null
        val listType = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(value, listType)
    }

    @TypeConverter
    fun toStringList(list: List<String>?): String? {
        return gson.toJson(list)
    }
    
    // Enum Converters
    @TypeConverter
    fun toUserRole(value: String) = enumValueOf<UserRole>(value)
    @TypeConverter
    fun fromUserRole(value: UserRole) = value.name

    @TypeConverter
    fun toGender(value: String) = enumValueOf<Gender>(value)
    @TypeConverter
    fun fromGender(value: Gender) = value.name

    // Add other enum converters if necessary, but Room can sometimes handle enums as Strings automatically if configured or we can rely on generic solution.
    // However, explicit is better.
    
    @TypeConverter
    fun toBookLoanStatus(value: String) = enumValueOf<BookLoanStatus>(value)
    @TypeConverter
    fun fromBookLoanStatus(value: BookLoanStatus) = value.name

    @TypeConverter
    fun toAttendanceStatus(value: String) = enumValueOf<AttendanceStatus>(value)
    @TypeConverter
    fun fromAttendanceStatus(value: AttendanceStatus) = value.name
    
    @TypeConverter
    fun toCheckInMethod(value: String) = enumValueOf<CheckInMethod>(value)
    @TypeConverter
    fun fromCheckInMethod(value: CheckInMethod) = value.name
    
    @TypeConverter
    fun toRuleCategory(value: String) = enumValueOf<RuleCategory>(value)
    @TypeConverter
    fun fromRuleCategory(value: RuleCategory) = value.name
    
    @TypeConverter
    fun toRuleSeverity(value: String) = enumValueOf<RuleSeverity>(value)
    @TypeConverter
    fun fromRuleSeverity(value: RuleSeverity) = value.name
    
    @TypeConverter
    fun toRecordStatus(value: String) = enumValueOf<RecordStatus>(value)
    @TypeConverter
    fun fromRecordStatus(value: RecordStatus) = value.name
    
    @TypeConverter
    fun toPetStatus(value: String) = enumValueOf<PetStatus>(value)
    @TypeConverter
    fun fromPetStatus(value: PetStatus) = value.name
    
    @TypeConverter
    fun toActivityType(value: String) = enumValueOf<ActivityType>(value)
    @TypeConverter
    fun fromActivityType(value: ActivityType) = value.name
    
    @TypeConverter
    fun toIncidentType(value: String) = enumValueOf<IncidentType>(value)
    @TypeConverter
    fun fromIncidentType(value: IncidentType) = value.name
    
    @TypeConverter
    fun toReportStatus(value: String) = enumValueOf<ReportStatus>(value)
    @TypeConverter
    fun fromReportStatus(value: ReportStatus) = value.name
    
    @TypeConverter
    fun toReportPriority(value: String) = enumValueOf<ReportPriority>(value)
    @TypeConverter
    fun fromReportPriority(value: ReportPriority) = value.name
    
    @TypeConverter
    fun toNotificationType(value: String) = enumValueOf<NotificationType>(value)
    @TypeConverter
    fun fromNotificationType(value: NotificationType) = value.name
    
    @TypeConverter
    fun toNotificationCategory(value: String) = enumValueOf<NotificationCategory>(value)
    @TypeConverter
    fun fromNotificationCategory(value: NotificationCategory) = value.name
}
