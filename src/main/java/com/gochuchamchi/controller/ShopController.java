@PostMapping("/register")
@PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
public String register(@RequestParam String brand,
                       @RequestParam String name,
                       @RequestParam String category,
                       @RequestParam int price,
                       @RequestParam(defaultValue = "0") int stock,
                       @RequestParam(required = false) String description,
                       @RequestParam(required = false) MultipartFile imageFile,
                       @RequestParam(required = false) List<String> sizes,
                       @AuthenticationPrincipal UserDetails userDetails) throws Exception {
    UserDto seller = userMapper.findByUsername(userDetails.getUsername());

    ProductDto product = new ProductDto();
    product.setSellerId(seller.getId());
    product.setBrand(brand);
    product.setName(name);
    product.setCategory(category);
    product.setPrice(price);
    product.setStock(stock);
    product.setDescription(description);

    if (imageFile != null && !imageFile.isEmpty()) {
        product.setImage(s3Service.upload(imageFile));
    }
    productMapper.insert(product);

    if (sizes != null && !sizes.isEmpty()) {
        List<ProductSizeDto> sizeList = new ArrayList<>();
        for (int i = 0; i < sizes.size(); i++) {
            ProductSizeDto sd = new ProductSizeDto();
            sd.setProductId(product.getId());
            sd.setSizeName(sizes.get(i));
            sd.setStock(0);
            sd.setSortOrder(i);
            sizeList.add(sd);
        }
        productSizeMapper.insertSizes(sizeList);
    }

    return "redirect:/shop";
}