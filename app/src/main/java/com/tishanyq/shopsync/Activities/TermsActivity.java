package com.tishanyq.shopsync.Activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.tishanyq.shopsync.R;

public class TermsActivity extends AppCompatActivity {
    
    private static final String PREFS_NAME = "ShopSyncPrefs";
    private static final String KEY_TERMS_ACCEPTED = "terms_accepted";
    
    private CheckBox checkboxAgree;
    private Button btnAccept;
    private Button btnDecline;
    private ScrollView scrollView;
    private boolean hasScrolledToBottom = false;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_terms);
        
        checkboxAgree = findViewById(R.id.checkboxAgree);
        btnAccept = findViewById(R.id.btnAccept);
        btnDecline = findViewById(R.id.btnDecline);
        scrollView = findViewById(R.id.scrollView);
        TextView tvTerms = findViewById(R.id.tvTermsContent);
        
        tvTerms.setText(getTermsAndConditions());
        
        btnAccept.setEnabled(false);
        
        scrollView.getViewTreeObserver().addOnScrollChangedListener(() -> {
            if (!hasScrolledToBottom) {
                View child = scrollView.getChildAt(0);
                if (child != null) {
                    int diff = child.getBottom() - (scrollView.getHeight() + scrollView.getScrollY());
                    if (diff <= 50) {
                        hasScrolledToBottom = true;
                    }
                }
            }
        });
        
        checkboxAgree.setOnCheckedChangeListener((buttonView, isChecked) -> {
            btnAccept.setEnabled(isChecked && hasScrolledToBottom);
            if (isChecked && !hasScrolledToBottom) {
                Toast.makeText(this, "Please scroll to read all terms before accepting", Toast.LENGTH_SHORT).show();
                checkboxAgree.setChecked(false);
            }
        });
        
        btnAccept.setOnClickListener(v -> {
            if (checkboxAgree.isChecked()) {
                saveTermsAccepted();
                proceedToNextScreen();
            }
        });
        
        btnDecline.setOnClickListener(v -> {
            Toast.makeText(this, "You must accept the Terms & Conditions to use ShopSync", Toast.LENGTH_LONG).show();
            finishAffinity();
        });
    }
    
    private void saveTermsAccepted() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        prefs.edit()
            .putBoolean(KEY_TERMS_ACCEPTED, true)
            .putLong("terms_accepted_at", System.currentTimeMillis())
            .apply();
    }
    
    public static boolean hasAcceptedTerms(android.content.Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return prefs.getBoolean(KEY_TERMS_ACCEPTED, false);
    }
    
    private void proceedToNextScreen() {
        Intent intent = new Intent(this, SplashActivity.class);
        startActivity(intent);
        finish();
    }
    
    private String getTermsAndConditions() {
        return "SHOPSYNC TERMS AND CONDITIONS\n\n" +
               "Last Updated: December 2024\n\n" +
               "PLEASE READ THESE TERMS AND CONDITIONS CAREFULLY BEFORE USING SHOPSYNC.\n\n" +
               
               "1. ACCEPTANCE OF TERMS\n\n" +
               "By downloading, installing, or using ShopSync (\"the App\"), you agree to be bound by these Terms and Conditions. If you do not agree to these terms, do not use the App.\n\n" +
               
               "2. SUBSCRIPTION AND PAYMENT\n\n" +
               "2.1. MONTHLY SUBSCRIPTION FEE\n" +
               "ShopSync operates on a paid subscription model. By using this App, you agree to pay a monthly subscription fee of TEN UNITED STATES DOLLARS (USD $10.00) per month.\n\n" +
               
               "2.2. PAYMENT TERMS\n" +
               "- Payment is due at the beginning of each billing cycle\n" +
               "- Your subscription begins upon activation of your product key\n" +
               "- Subscription fees are non-refundable\n" +
               "- Payment must be made through approved payment methods\n\n" +
               
               "2.3. FAILURE TO PAY\n" +
               "If payment is not received within the grace period:\n" +
               "- Your access to ShopSync will be suspended\n" +
               "- Your data will be retained for 30 days after suspension\n" +
               "- After 30 days of non-payment, your account and all associated data may be permanently deleted\n" +
               "- Reactivation requires payment of all outstanding fees\n\n" +
               
               "3. LICENSE AND PRODUCT KEY\n\n" +
               "3.1. Each product key is valid for ONE (1) shop and up to THREE (3) devices\n" +
               "3.2. Product keys are non-transferable\n" +
               "3.3. Sharing product keys with unauthorized users is prohibited\n" +
               "3.4. License validity is for 30 days from activation date\n\n" +
               
               "4. SERVICE DESCRIPTION\n\n" +
               "ShopSync provides:\n" +
               "- Inventory management\n" +
               "- Sales tracking and reporting\n" +
               "- Debt management\n" +
               "- Multi-device synchronization\n" +
               "- Data backup and security\n\n" +
               
               "5. USER RESPONSIBILITIES\n\n" +
               "5.1. You are responsible for:\n" +
               "- Maintaining the confidentiality of your PIN and product key\n" +
               "- All activities conducted under your account\n" +
               "- Ensuring accurate data entry\n" +
               "- Timely payment of subscription fees\n\n" +
               
               "5.2. You agree NOT to:\n" +
               "- Reverse engineer, modify, or create derivative works of the App\n" +
               "- Use the App for any illegal or unauthorized purpose\n" +
               "- Share your credentials with unauthorized persons\n" +
               "- Attempt to bypass security measures\n\n" +
               
               "6. DATA PRIVACY\n\n" +
               "6.1. We collect and process business data necessary for App functionality\n" +
               "6.2. Your data is encrypted and securely stored\n" +
               "6.3. We do not sell your data to third parties\n" +
               "6.4. Data may be retained for legal compliance purposes\n\n" +
               
               "7. SERVICE AVAILABILITY\n\n" +
               "7.1. We strive for 99.9% uptime but do not guarantee uninterrupted service\n" +
               "7.2. Scheduled maintenance will be communicated in advance when possible\n" +
               "7.3. We are not liable for data loss due to service interruptions\n\n" +
               
               "8. LIMITATION OF LIABILITY\n\n" +
               "8.1. ShopSync is provided \"as is\" without warranties of any kind\n" +
               "8.2. We are not liable for indirect, incidental, or consequential damages\n" +
               "8.3. Our liability is limited to the amount of fees paid by you\n\n" +
               
               "9. TERMINATION\n\n" +
               "9.1. You may terminate your subscription at any time\n" +
               "9.2. We may terminate your account for violation of these terms\n" +
               "9.3. Upon termination, access to the App and data will be revoked\n\n" +
               
               "10. CHANGES TO TERMS\n\n" +
               "We reserve the right to modify these terms at any time. Continued use of the App constitutes acceptance of updated terms.\n\n" +
               
               "11. CONTACT INFORMATION\n\n" +
               "For support or inquiries:\n" +
               "Email: support@shopsync.app\n\n" +
               
               "BY CHECKING THE BOX BELOW, YOU ACKNOWLEDGE THAT YOU HAVE READ, UNDERSTOOD, AND AGREE TO BE BOUND BY THESE TERMS AND CONDITIONS, INCLUDING THE MONTHLY SUBSCRIPTION FEE OF $10.00 USD.";
    }
    
    @Override
    public void onBackPressed() {
        Toast.makeText(this, "You must accept the Terms & Conditions to continue", Toast.LENGTH_SHORT).show();
    }
}
