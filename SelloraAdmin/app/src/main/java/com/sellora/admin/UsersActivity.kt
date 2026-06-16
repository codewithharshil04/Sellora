package com.sellora.admin

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.sellora.admin.databinding.ActivityUsersBinding
import com.sellora.admin.databinding.ItemAdminUserBinding

class UsersActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUsersBinding
    private val viewModel: UsersViewModel by viewModels()
    private lateinit var adapter: UserAdapter
    private var allUsers = listOf<AdminUser>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUsersBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }

        binding.rvUsers.layoutManager = LinearLayoutManager(this)
        binding.rvUsers.itemAnimator = null
        adapter = UserAdapter()
        binding.rvUsers.adapter = adapter

        setupObservers()

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { filter(s.toString()) }
            override fun afterTextChanged(s: Editable?) {}
        })

        NavigationUtils.setupBottomNavigation(this, binding.bottomNav, R.id.nav_users)

        binding.swipeRefresh.setColorSchemeResources(R.color.sellora_primary)
        binding.swipeRefresh.setOnRefreshListener { viewModel.fetchUsers() }

        viewModel.fetchUsers()
    }

    private fun setupObservers() {
        viewModel.users.observe(this) { users ->
            allUsers = users
            binding.txtCount.text = "${allUsers.size} users"
            binding.swipeRefresh.isRefreshing = false
            filter(binding.etSearch.text.toString())
        }

        viewModel.isLoading.observe(this) { isLoading ->
            if (!binding.swipeRefresh.isRefreshing) {
                binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            }
        }

        viewModel.error.observe(this) { error ->
            error?.let {
                binding.swipeRefresh.isRefreshing = false
                Toast.makeText(this, "Error: $it", Toast.LENGTH_SHORT).show()
                viewModel.clearError()
            }
        }
    }

    private fun filter(query: String) {
        val filtered = if (query.isEmpty()) allUsers
        else allUsers.filter {
            it.name.contains(query, true) || it.email.contains(query, true)
        }
        adapter.submitList(filtered)
        binding.txtEmpty.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
    }
}

class UserAdapter : ListAdapter<AdminUser, UserAdapter.VH>(DIFF) {

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<AdminUser>() {
            override fun areItemsTheSame(a: AdminUser, b: AdminUser) = a.id == b.id
            override fun areContentsTheSame(a: AdminUser, b: AdminUser) = a == b
        }
    }

    inner class VH(val binding: ItemAdminUserBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        return VH(ItemAdminUserBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = getItem(position)
        holder.binding.txtName.text = item.name.ifEmpty { "—" }
        holder.binding.txtEmail.text = item.email.ifEmpty { "—" }
        holder.binding.txtPhone.text = item.phone.ifEmpty { "—" }
        holder.binding.txtAvatar.text = item.name.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
        MotionUtils.animateItemEntry(holder.itemView, position)
    }
}
