package in.mapmytour.auth.repository;

import in.mapmytour.auth.entity.GroupExpense;
import in.mapmytour.auth.entity.TravelGroup;
import in.mapmytour.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GroupExpenseRepository extends JpaRepository<GroupExpense, String> {
    List<GroupExpense> findByGroupOrderByExpenseDateDesc(TravelGroup group);
    List<GroupExpense> findByGroupAndStatusOrderByExpenseDateDesc(TravelGroup group, String status);
    List<GroupExpense> findByPaidBy(User user);
}

