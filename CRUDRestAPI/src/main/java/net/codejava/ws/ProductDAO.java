package net.codejava.ws;

import java.util.ArrayList;
import java.util.List;

public class ProductDAO {
	private static ProductDAO instance;
	private static List<Product> data =new ArrayList<>();
	
	
	static {
		data.add(new Product(1, "iphone XI",999.99f));
		data.add(new Product(2, "X BOX 360",329.50f));
	}
	
	
	private ProductDAO() {}
	
	public static ProductDAO getInstance() {
		if(instance == null) {
			instance = new ProductDAO();
		}
		return instance;
		
	}
	
	public List<Product>listAll(){
		return new ArrayList<Product>(data);
	}
	
	
	public int add(Product product) {
		
		int newID= data.size() +1 ;
		product.setId(newID);
		data.add(product);
		
		return newID;
	}
	
	public Product get(int id) {
		Product productToFind = new Product(id);
		int index = data.indexOf(productToFind);
		if(index >=0) {
			return data.get(index);
			
		}
		return null;
		
	}
	
	public boolean update(Product product) {
		int index = data.indexOf(product);
		if(index >=0) {
			data.set(index, product);
			return true;	
		}
		return false;
	}
	
	public boolean delete(int id) {
		Product productToFind = new Product(id);
		int index = data.indexOf(productToFind);
		if(index >=0) {
		 data.remove(index);
		 return true;
		}
		return false;
	}

	
	
}
