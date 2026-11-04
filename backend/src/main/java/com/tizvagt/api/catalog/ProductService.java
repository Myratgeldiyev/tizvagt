package com.tizvagt.api.catalog;

import com.tizvagt.api.common.PageResponse;
import com.tizvagt.api.common.ResourceNotFoundException;
import com.tizvagt.api.storage.FileStorageService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final FileStorageService fileStorageService;

    public ProductService(ProductRepository productRepository, FileStorageService fileStorageService) {
        this.productRepository = productRepository;
        this.fileStorageService = fileStorageService;
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> search(ProductFilter filter, Pageable pageable) {
        Specification<Product> spec = ProductSpecifications.all();
        if (StringUtils.hasText(filter.search())) {
            spec = spec.and(ProductSpecifications.nameContains(filter.search()));
        }
        if (filter.categoryId() != null) {
            spec = spec.and(ProductSpecifications.inCategory(filter.categoryId()));
        }
        if (filter.minPrice() != null) {
            spec = spec.and(ProductSpecifications.priceAtLeast(filter.minPrice()));
        }
        if (filter.maxPrice() != null) {
            spec = spec.and(ProductSpecifications.priceAtMost(filter.maxPrice()));
        }
        return PageResponse.from(productRepository.findAll(spec, pageable), ProductResponse::from);
    }

    @Transactional(readOnly = true)
    public ProductResponse getById(Long id) {
        return ProductResponse.from(findProduct(id));
    }

    @Transactional
    public ProductResponse updateImage(Long id, MultipartFile file) {
        Product product = findProduct(id);
        String oldImage = product.getImageUrl();

        String newImage = fileStorageService.storeImage(file);
        product.changeImage(newImage);

        if (oldImage != null) {
            fileStorageService.delete(oldImage);
        }
        return ProductResponse.from(product);
    }

    private Product findProduct(Long id) {
        return productRepository.findWithCategoryById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));
    }
}
