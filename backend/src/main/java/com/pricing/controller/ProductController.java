package com.pricing.controller;

import com.pricing.common.CommonResult;
import com.pricing.entity.Product;
import com.pricing.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@CrossOrigin(origins = "*") // For local development flexibility, though controlled in production
public class ProductController {

    @Autowired
    private ProductService productService;

    @GetMapping
    public CommonResult<List<Product>> list() {
        return CommonResult.success(productService.list());
    }

    @GetMapping("/{id}")
    public CommonResult<Product> getById(@PathVariable Long id) {
        return CommonResult.success(productService.getById(id));
    }

    @PostMapping
    public CommonResult<Boolean> save(@RequestBody Product product) {
        return CommonResult.success(productService.save(product));
    }

    @PutMapping
    public CommonResult<Boolean> update(@RequestBody Product product) {
        return CommonResult.success(productService.updateById(product));
    }

    @DeleteMapping("/{id}")
    public CommonResult<Boolean> delete(@PathVariable Long id) {
        return CommonResult.success(productService.removeById(id));
    }
}
