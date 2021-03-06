package com.sohel.themelookassignment.views.seller;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;


import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.sohel.themelookassignment.R;
import com.sohel.themelookassignment.adapter.ImageSliderAdapter;
import com.sohel.themelookassignment.adapter.SpinnerCustomAdapter;
import com.sohel.themelookassignment.api.ApiRef;
import com.sohel.themelookassignment.handler.AppBar;
import com.sohel.themelookassignment.localdb.UserDb;
import com.sohel.themelookassignment.model.Brand;
import com.sohel.themelookassignment.model.Category;
import com.sohel.themelookassignment.model.ImageModel;
import com.sohel.themelookassignment.model.Product;
import com.sohel.themelookassignment.model.ProductPrice;
import com.sohel.themelookassignment.model.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class SellerAddProductActivity extends AppCompatActivity {

    public static final int PICK_IMAGE=100;
    private EditText productNameEt,productDescriptionEt;
    private RecyclerView imageRecyclerView;
    private Button chooseImageButton,uploadProductButton;
    private Spinner brandSpinner,categorySpinner;
    private TextView addProductVariantTv;

    private LinearLayout imageUploadSectionLayout,productVariantLn;
    private ProgressBar imageUploadProgress,loadingProgress;
    private TextView completeUploadTvId;
    ArrayList imageList = new ArrayList();
    ArrayList<ImageModel> imageModelArrayList=new ArrayList<>();
    ImageSliderAdapter sliderAdapter;
    List<Brand> brandList=new ArrayList<>();
    List<Category> categoryList=new ArrayList<>();
    UserDb userDb;

    //Database Refrence

    private StorageReference storageReference;
    private ProgressDialog progressDialog;

    private String selectedCategory="",selectedBrand="";
    private List<View> productVariantViewList=new ArrayList<>();
    private List<ProductPrice> productPriceList=new ArrayList<>();
    private boolean noErrorInVariant=false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_seller_add_product);
        init();


        chooseImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                startActivityForResult(intent, PICK_IMAGE);
            }
        });
        brandSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Brand brand=brandList.get(position);
                selectedBrand=brand.getBrandName();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        categorySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Category category=categoryList.get(position);
              selectedCategory=category.getCategoryName();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        uploadProductButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String productName=productNameEt.getText().toString();
                String productDescription=productDescriptionEt.getText().toString();

                if(imageList.size()==0){
                    Toast.makeText(SellerAddProductActivity.this, "Please Choose Some Images For Your Product.", Toast.LENGTH_SHORT).show();
                }else if(productName.isEmpty()){
                    productNameEt.setError("Enter Product Name.");
                    productNameEt.requestFocus();
                }else if(productVariantViewList.size()==0){
                    Toast.makeText(SellerAddProductActivity.this, "Please Add Atlease One Variant For Your Product", Toast.LENGTH_SHORT).show();
                }
                else if(productDescription.isEmpty()){
                    productDescriptionEt.setError("Enter Product Description.");
                    productDescriptionEt.requestFocus();
                }else if(selectedBrand.isEmpty()){
                    Toast.makeText(SellerAddProductActivity.this, "Please Select A Brand For Your Product", Toast.LENGTH_SHORT).show();
                }else if(selectedCategory.isEmpty()){
                    Toast.makeText(SellerAddProductActivity.this, "Please Select A Category For Your Product", Toast.LENGTH_SHORT).show();
                }else{

                    setProductPrice();
                    if(productPriceList.size()==productVariantViewList.size()){
                        loadingProgress.setVisibility(View.VISIBLE);
                        imageUploadSectionLayout.setVisibility(View.VISIBLE);
                        completeUploadTvId.setText("0/"+imageList.size());
                        uploadProductButton.setText("Uploading Image.....");
                        uploadProductButton.setEnabled(false);


                        uploadImageToStorage(productName,productDescription);
                    }
                    //all is perfect lets upload product.

                }


            }
        });

        addProductVariantTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addProductVariantView();
            }
        });


    }


    @Override
    protected void onStart() {
        super.onStart();
        getBrandList();
        getCategoryList();
    }
    private void init(){
        //initalize storage
        storageReference= FirebaseStorage.getInstance().getReference().child("ProductImages");
        progressDialog=new ProgressDialog(this);
        userDb=new UserDb(this);
        brandSpinner=findViewById(R.id.s_ap_selectProductBrand);
        categorySpinner=findViewById(R.id.s_ap_selectProductCategory);
        productNameEt=findViewById(R.id.s_ap_productNameEt);
        productDescriptionEt=findViewById(R.id.s_ap_productDescription);
        chooseImageButton=findViewById(R.id.s_ap_chooseImageButton);
        uploadProductButton=findViewById(R.id.s_ap_uploadProductButton);
        imageUploadSectionLayout=findViewById(R.id.imageUploadLayout);
        imageUploadProgress=findViewById(R.id.imageUploadProgressBarId);
        loadingProgress=findViewById(R.id.loadingProgressBar);
        completeUploadTvId=findViewById(R.id.completeUploadTvId);
        addProductVariantTv=findViewById(R.id.addProductVariantTv);
        productVariantLn=findViewById(R.id.product_variantList_Ln);


        imageRecyclerView=findViewById(R.id.s_ap_selectedImaegRecyclerViewId);
        imageRecyclerView.setHasFixedSize(true);
        LinearLayoutManager layoutManager=new LinearLayoutManager(this);
        layoutManager.setOrientation(RecyclerView.HORIZONTAL);
        imageRecyclerView.setLayoutManager(layoutManager);
        //setup Toolbar
        Toolbar toolbar = findViewById(R.id.appBarId);
        AppBar.setUpAppBar(this,toolbar,"Add New Product");

    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE) {
            if (resultCode == RESULT_OK) {
                imageList.clear();
                if (data.getClipData() != null) {
                    int countClipData = data.getClipData().getItemCount();
                    int currentImageSlect = 0;
                    while (currentImageSlect < countClipData) {
                        Uri imageUri = data.getClipData().getItemAt(currentImageSlect).getUri();
                        imageList.add(imageUri);
                        currentImageSlect = currentImageSlect + 1;
                    }
                    chooseImageButton.setText(imageList.size()+" Image Selected.");
                } else {
                    imageList.add(data.getData());
                    chooseImageButton.setText(1+" Image Selected.");
                }
                imageRecyclerView.setVisibility(View.VISIBLE);
                imageRecyclerView.setVisibility(View.VISIBLE);
                sliderAdapter=new ImageSliderAdapter(SellerAddProductActivity.this,imageList);
                imageRecyclerView.setAdapter(sliderAdapter);
            }
        }
    }

    private void getBrandList(){
        ApiRef.getBrandRef()
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if(snapshot.exists()){
                            brandList.clear();
                            for(DataSnapshot snapshot1:snapshot.getChildren()){
                                Brand brand=snapshot1.getValue(Brand.class);
                                brandList.add(brand);
                            }
                            SpinnerCustomAdapter brandAdapter=new SpinnerCustomAdapter(SellerAddProductActivity.this,brandList,null,1);
                            brandSpinner.setAdapter(brandAdapter);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }
    private void getCategoryList(){
        ApiRef.getCategoryRef()
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if(snapshot.exists()){
                            categoryList.clear();
                            for(DataSnapshot snapshot1:snapshot.getChildren()){
                                Category category=snapshot1.getValue(Category.class);
                                categoryList.add(category);
                            }
                            SpinnerCustomAdapter categoryAdapter=new SpinnerCustomAdapter(SellerAddProductActivity.this,null,categoryList,2);
                            categorySpinner.setAdapter(categoryAdapter);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }
    public String getFileExtension(Uri imageuri){
        ContentResolver contentResolver=getContentResolver();
        MimeTypeMap mimeTypeMap=MimeTypeMap.getSingleton();
        return  mimeTypeMap.getExtensionFromMimeType(contentResolver.getType(imageuri));
    }
    private void uploadImageToStorage(String productName,String productDescription) {
           imageModelArrayList.clear();
           /* progressDialog.setTitle("Uploading Image");
            progressDialog.setCancelable(false);
            progressDialog.setCanceledOnTouchOutside(false);
            progressDialog.setMessage("Uploaded "+0+"/"+imageList.size());
            progressDialog.show();*/
            for (int i = 0; i < imageList.size(); i++) {
                StorageReference filePath = storageReference.child(System.currentTimeMillis() + new Random().nextInt() + "." + getFileExtension(Uri.parse(imageList.get(i).toString())));
                int finalI = i;
                filePath.putFile(Uri.parse(imageList.get(i).toString())).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        Task<Uri> urlTask = taskSnapshot.getStorage().getDownloadUrl();
                        while (!urlTask.isSuccessful()) ;
                        Uri downloaduri = urlTask.getResult();

                        double progress=((finalI+1)*imageList.size()/100)*100;
                        imageUploadProgress.setProgress(finalI*10);
                        completeUploadTvId.setText((finalI+1)+"/"+imageList.size());
                        ImageModel imageModel = new ImageModel(downloaduri.toString());
                        imageModelArrayList.add(imageModel);

                        if(finalI==imageList.size()-1){
                            Toast.makeText(SellerAddProductActivity.this, "Image Uploaded.", Toast.LENGTH_SHORT).show();
                            imageUploadSectionLayout.setVisibility(View.GONE);
                            uploadProductButton.setText("Saving Product..");
                            saveProductInstanceToDatabase(productName,productDescription);
                        }

                    }
                });
            }


    }
    private void saveProductInstanceToDatabase(String productName,String productDescription) {
        String productId=ApiRef.getProductRef().push().getKey()+System.currentTimeMillis();
        User currentUser=userDb.getUserData();
       Product product=new Product(productId,currentUser.getUserId(),productName,productDescription,selectedBrand,selectedCategory,productPriceList,0,imageModelArrayList);
        ApiRef.getProductRef()
                .child(productId)
                .setValue(product)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        clearAllProductVariantView();
                        imageList.clear();
                        imageModelArrayList.clear();
                        imageRecyclerView.setVisibility(View.GONE);
                        productNameEt.setText("");
                        productDescriptionEt.setText("");
                        selectedBrand="";
                        selectedCategory="";
                        uploadProductButton.setText("Upload Product");
                        uploadProductButton.setEnabled(true);
                        chooseImageButton.setText("Choose Image.");
                        loadingProgress.setVisibility(View.GONE);
                        Toast.makeText(SellerAddProductActivity.this, "Product Saved Successful..", Toast.LENGTH_SHORT).show();
                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(SellerAddProductActivity.this, "Product Save Failed.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void clearAllProductVariantView() {
        productPriceList.clear();
        if(productVariantViewList.size()>0){
            for(View view:productVariantViewList){
                productVariantLn.removeView(view);
            }
            productVariantViewList.clear();
        }
    }


    private void addProductVariantView(){
        View view=getLayoutInflater().inflate(R.layout.product_price_item_layout,null,false);

        ImageView closeBtn=view.findViewById(R.id.pp_closeImg);
        closeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                removeProductVariantView(view);
            }
        });

        productVariantViewList.add(view);
        productVariantLn.addView(view);

    }
    private void removeProductVariantView(View view){
        productVariantLn.removeView(view);
        productVariantViewList.remove(view);
    }
    private void setProductPrice() {
        productPriceList.clear();
        if(productVariantViewList.size()>0){
            for(View view:productVariantViewList){
                EditText sizeEt=view.findViewById(R.id.pp_sizeEt);
                EditText priceEt=view.findViewById(R.id.pp_priceEt);
                EditText colorEt=view.findViewById(R.id.pp_colorEt);

                String size=sizeEt.getText().toString();
                String price=priceEt.getText().toString();
                String color=colorEt.getText().toString();
                if(color.isEmpty()){
                    colorEt.setError("Enter Color.");
                    colorEt.requestFocus();
                }else if(size.isEmpty()){
                    sizeEt.setError("Enter Size.");
                    sizeEt.requestFocus();
                }else if(price.isEmpty()){
                    priceEt.setError("Enter Price");
                    priceEt.requestFocus();
                }else{
                    ProductPrice productPrice=new ProductPrice(color,size,Double.parseDouble(price));
                    productPriceList.add(productPrice);
                }
            }
        }
    }
}