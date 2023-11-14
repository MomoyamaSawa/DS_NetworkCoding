package service.info.infoservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import service.info.infoservice.pojo.Info;

public interface InfoRepository extends JpaRepository<Info, String> {
    boolean existsByFileName(String fileName);
}