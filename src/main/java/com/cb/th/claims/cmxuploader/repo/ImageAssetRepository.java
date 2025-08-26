package com.cb.th.claims.cmxuploader.repo;

import com.cb.th.claims.cmxuploader.cenum.ImageKind;
import com.cb.th.claims.cmxuploader.domain.ImageAsset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ImageAssetRepository extends JpaRepository<ImageAsset, Long> {

  // order of params must match property order in the name
  boolean existsByFnolReferenceNoAndChecksumSha256AndKind(
      String fnolReferenceNo,
      String checksumSha256,
      ImageKind kind
  );

  List<ImageAsset> findAllByFnolReferenceNo(String fnolReferenceNo);
}
