package in.mapmytour.auth.repository;

import in.mapmytour.auth.entity.Document;
import in.mapmytour.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentRepository extends JpaRepository<Document, String> {

    List<Document> findByUserOrderByUploadDateDesc(User user);

    Optional<Document> findByIdAndUser(String id, User user);

    void deleteByIdAndUser(String id, User user);
}

