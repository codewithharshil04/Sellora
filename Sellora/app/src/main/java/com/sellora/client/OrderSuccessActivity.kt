package com.sellora.client

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.sellora.client.databinding.ActivityOrderSuccessBinding
import java.text.SimpleDateFormat
import java.util.*

class OrderSuccessActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOrderSuccessBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOrderSuccessBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val orderId        = intent.getStringExtra("order_id")        ?: "Order ID"
        val serviceName    = intent.getStringExtra("service_name")    ?: "Service"
        val freelancerName = intent.getStringExtra("freelancer_name") ?: "Freelancer"
        val price          = intent.getStringExtra("price")           ?: "₹0"

        val now = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date())

        // Bind views
        binding.txtTotalAmount.text = price
        binding.txtProjectId.text   = orderId
        binding.txtStatus.text      = "Pending"
        binding.txtPlacedOn.text    = now

        // C13: Close button — goes back to home
        binding.btnClose.setOnClickListener {
            val intent = Intent(this, HomeActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
        }

        binding.btnViewProjects.setOnClickListener {
            startActivity(Intent(this, ProjectsActivity::class.java))
            finish()
        }

        binding.btnExplore.setOnClickListener {
            val intent = Intent(this, HomeActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
        }
    }
}