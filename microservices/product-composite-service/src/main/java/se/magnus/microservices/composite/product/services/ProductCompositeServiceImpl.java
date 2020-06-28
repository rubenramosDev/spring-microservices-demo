package se.magnus.microservices.composite.product.services;

import java.util.List;
import java.util.stream.Collectors;

import se.magnus.api.composite.ProductAggregate;
import se.magnus.api.composite.ProductCompositeService;
import se.magnus.api.composite.RecommendationSummary;
import se.magnus.api.composite.ReviewSummary;
import se.magnus.api.composite.ServiceAddresses;
import se.magnus.api.core.product.Product;
import se.magnus.api.core.recommendation.Recommendation;
import se.magnus.api.core.review.Review;
import se.magnus.util.exceptions.NotFoundException;
import se.magnus.util.http.ServiceUtil;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProductCompositeServiceImpl implements ProductCompositeService {

	private final ServiceUtil serviceUtil;

	private ProductCompositeIntegration integration;

	@Autowired
	public ProductCompositeServiceImpl(ServiceUtil serviceUtil,
			ProductCompositeIntegration productCompositeIntegration){
		this.serviceUtil = serviceUtil;
		this.integration = productCompositeIntegration;
	}

	@Override
	public ProductAggregate getProduct(int productId) {

		Product product = integration.getProduct(productId);
		if(product == null) throw new NotFoundException("No product found for product id: "+ productId);

		List<Recommendation> recommendations = integration.getRecommendations(productId);

		List<Review> reviews = integration.getReviews(productId);


		return createProductAggregate(product, recommendations, reviews, serviceUtil.getServiceAddress());
	}

	private ProductAggregate createProductAggregate(Product product, List<Recommendation> recommendations, List<Review> reviews, String serviceAddress) {

		int productId = product.getProductId();
		String name = product.getName();
		int weight = product.getWeight();

		List<RecommendationSummary> recommendationSummaries = (recommendations == null) ? null :
				recommendations.stream()
					.map(r -> new RecommendationSummary(r.getProductId(), r.getAuthor(), r.getRate()))
					.collect(Collectors.toList());

		List<ReviewSummary> reviewSummaries = (reviews == null)  ? null :
				reviews.stream()
						.map(r -> new ReviewSummary(r.getReviewId(), r.getAuthor(), r.getSubject()))
						.collect(Collectors.toList());

		String productAddress = product.getServiceAddress();
		String reviewAddress = (reviews != null && reviews.size() > 0) ? reviews.get(0).getServiceAddress() : "";
		String recommendationAddress = (recommendations != null && recommendations.size() > 0) ? recommendations.get(0).getServiceAddress() : "";
		ServiceAddresses serviceAddresses = new ServiceAddresses(serviceAddress, productAddress, reviewAddress, recommendationAddress);

		return new ProductAggregate(productId, name, weight, recommendationSummaries, reviewSummaries, serviceAddresses);
	}
}
