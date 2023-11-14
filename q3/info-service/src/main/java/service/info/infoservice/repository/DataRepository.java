package service.info.infoservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import service.info.infoservice.pojo.Data;

public interface DataRepository extends JpaRepository<Data, String> {
}