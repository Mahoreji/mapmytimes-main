package in.mapmytour.auth.repository;

import in.mapmytour.auth.entity.ExpenseParticipant;
import in.mapmytour.auth.entity.GroupExpense;
import in.mapmytour.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExpenseParticipantRepository extends JpaRepository<ExpenseParticipant, String> {
    List<ExpenseParticipant> findByExpense(GroupExpense expense);
    List<ExpenseParticipant> findByUser(User user);
    Optional<ExpenseParticipant> findByExpenseAndUser(GroupExpense expense, User user);
    List<ExpenseParticipant> findByUserAndStatus(User user, String status);
}

