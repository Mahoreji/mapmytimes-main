package in.mapmytour.auth.repository;

import in.mapmytour.auth.entity.BookingAttribution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BookingAttributionRepository extends JpaRepository<BookingAttribution, String> {

    Optional<BookingAttribution> findByBookingId(String bookingId);
}
