package service.info.infoservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import service.info.infoservice.pojo.Info;

@Repository
public interface InfoRepository extends JpaRepository<Info, String> {
    boolean existsByFilename(String filename);
}