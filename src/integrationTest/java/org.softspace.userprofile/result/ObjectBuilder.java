package org.softspace.userprofile.result;

import org.softspace.userprofile.enums.ProfileStatus;
import org.softspace.userprofile.result.object.UserProfileDbResult;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.UUID;

public class ObjectBuilder {

    public static UserProfileDbResult getUserProfileDbResult(ResultSet rs) throws SQLException {
        Timestamp deletedAt = rs.getTimestamp("deleted_at");

        return new UserProfileDbResult(
                rs.getObject("guid", UUID.class),
                rs.getString("name"),
                rs.getString("surname"),
                rs.getString("middle_name"),
                rs.getObject("date_of_birth", LocalDate.class),
                rs.getString("email"),
                rs.getString("phone"),
                ProfileStatus.valueOf(rs.getString("profile_status")),
                rs.getTimestamp("updated_at").toInstant(),
                rs.getTimestamp("created_at").toInstant(),
                deletedAt != null ? deletedAt.toInstant() : null
        );
    }
}
