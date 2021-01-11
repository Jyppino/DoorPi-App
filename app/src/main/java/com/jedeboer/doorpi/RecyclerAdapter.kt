package com.jedeboer.doorpi

import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.RecyclerView
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class RecyclerAdapter(val keys: MutableList<Key>) : RecyclerView.Adapter<KeyHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): KeyHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.card_layout, parent, false)
        return KeyHolder(view)
    }

    override fun getItemCount(): Int {
        return keys.size
    }

    override fun onBindViewHolder(holder: KeyHolder, position: Int) {
        holder.setKey(keys[position])
    }
}

class KeyHolder(v: View) : RecyclerView.ViewHolder(v), View.OnClickListener {
    private var view: View = v
    private lateinit var key: Key
    val name: TextView
    private val latest: TextView
    private val unlocks: TextView
    private val created: TextView
    private val options: TextView
    private val admin: ImageView

    init {
        v.setOnClickListener(this)
        unlocks = view.findViewById(R.id.key_unlocks)
        name = view.findViewById(R.id.key_name)
        latest = view.findViewById(R.id.key_latest)
        options = view.findViewById(R.id.key_options)
        admin = view.findViewById(R.id.key_admin)
        created = view.findViewById(R.id.key_created)

        view.findViewById<TextView>(R.id.key_options).setOnClickListener { optionsMenu() }
    }

    fun setKey(newKey: Key) {
        key = newKey
        name.text = key.name
        unlocks.text = key.unlocks.toString()
        view.findViewById<ImageView>(R.id.key_user).visibility =
            if (key.isCurrertUser) View.VISIBLE else View.GONE
        setAdmin(key.admin)

        val formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss")
        created.text = formatter.format(key.created)
        if (key.latestUnlock != null) {
            latest.text = formatter.format(key.latestUnlock)
        } else {
            latest.text = "-"
        }
    }

    private fun optionsMenu() {
        val popup = PopupMenu(view.context, options)
        popup.inflate(R.menu.menu_key)
        popup.menu.findItem(R.id.key_action_demote).isVisible = key.admin
        popup.menu.findItem(R.id.key_action_promote).isVisible = !key.admin
        popup.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.key_action_delete -> {
                    val confirmDialog = AlertDialog.Builder(key.parent)
                    confirmDialog.setTitle("Confirm")
                    confirmDialog.setMessage("Delete ${key.name}?")
                    confirmDialog.setPositiveButton(R.string.key_confirm_yes) { _, _ ->
                        key.parent.deleteKey(
                            key
                        )
                    }
                    confirmDialog.setNegativeButton(R.string.key_confirm_no) { _, _ -> }
                    confirmDialog.show()
                }
                R.id.key_action_promote -> {
                    val confirmDialog = AlertDialog.Builder(key.parent)
                    confirmDialog.setTitle("Confirm")
                    confirmDialog.setMessage("Give admin rights to ${key.name}?")
                    confirmDialog.setPositiveButton(R.string.key_confirm_yes) { _, _ ->
                        key.parent.updateAdmin(
                            this,
                            key,
                            true
                        )
                    }
                    confirmDialog.setNegativeButton(R.string.key_confirm_no) { _, _ -> }
                    confirmDialog.show()
                }
                R.id.key_action_demote -> {
                    val confirmDialog = AlertDialog.Builder(key.parent)
                    confirmDialog.setTitle("Confirm")
                    confirmDialog.setMessage("Revoke admin rights from ${key.name}?")
                    confirmDialog.setPositiveButton(R.string.key_confirm_yes) { _, _ ->
                        key.parent.updateAdmin(
                            this,
                            key,
                            false
                        )
                    }
                    confirmDialog.setNegativeButton(R.string.key_confirm_no) { _, _ -> }
                    confirmDialog.show()
                }
                R.id.key_action_rename -> {
                    val confirmDialog = AlertDialog.Builder(key.parent)
                    val layout = LinearLayout(key.parent)
                    val newName = EditText(key.parent)
                    val layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )

                    layoutParams.setMargins(70, 0, 70, 0)
                    newName.layoutParams = layoutParams
                    newName.inputType = InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                    newName.requestFocus()
                    layout.addView(newName)

                    confirmDialog.setTitle("Rename")
                    confirmDialog.setMessage(R.string.action_rename_text)
                    confirmDialog.setView(layout)
                    confirmDialog.setPositiveButton(R.string.key_confirm_submit) { _, _ ->
                        key.parent.updateName(this, key, newName.text.toString())
                    }
                    confirmDialog.setNegativeButton(R.string.key_confirm_cancel) { _, _ -> }
                    val d = confirmDialog.create()
                    d.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
                    d.show()
                }
            }
            true
        }
        popup.show()
    }

    fun setAdmin(status: Boolean) {
        key.admin = status
        admin.visibility = if (key.admin) View.VISIBLE else View.INVISIBLE
    }

    fun setName(name: String) {
        key.name = name
    }

    override fun onClick(v: View) {}
}

class Key(
    val id: String,
    var name: String,
    val unlocks: Number,
    val latestUnlock: LocalDateTime?,
    var admin: Boolean,
    val parent: ManageActivity,
    val isCurrertUser: Boolean,
    val created: LocalDateTime
)