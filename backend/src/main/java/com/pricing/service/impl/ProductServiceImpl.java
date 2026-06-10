package com.pricing.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pricing.entity.Product;
import com.pricing.mapper.ProductMapper;
import com.pricing.service.ProductService;
import org.springframework.stereotype.Service;

@Service
public class ProductServiceImpl extends ServiceImpl<ProductMapper, Product> implements ProductService {
}
