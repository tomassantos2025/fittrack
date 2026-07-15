package com.example.fittrack.ui

import android.animation.ValueAnimator
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.fittrack.R
import com.example.fittrack.databinding.ActivityHelpBinding
import com.example.fittrack.utils.LocaleHelper

/**
 * Ecrã de ajuda. Explica como usar a aplicação e cumpre o requisito de help/start menu.
 *
 * Comentário de manutenção: esta classe deve manter a lógica separada por responsabilidade,
 * para ser mais fácil alterar a app sem quebrar outros ecrãs.
 */
class HelpActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHelpBinding

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHelpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.help_title)
        binding.toolbar.setNavigationOnClickListener { finish() }

        val items = listOf(
            getString(R.string.help_create_plan_title) to getString(R.string.help_create_plan_body),
            getString(R.string.help_log_session_title) to getString(R.string.help_log_session_body),
            getString(R.string.help_charts_title) to getString(R.string.help_charts_body),
            getString(R.string.help_pro_title) to getString(R.string.help_pro_body)
        )

        items.forEach { (title, body) ->
            addAccordionItem(binding.llAccordion, title, body)
        }
    }

    private fun addAccordionItem(container: LinearLayout, title: String, body: String) {
        val itemView = LayoutInflater.from(this)
            .inflate(R.layout.item_help_accordion, container, false)

        val tvTitle = itemView.findViewById<TextView>(R.id.tvTitle)
        val tvBody = itemView.findViewById<TextView>(R.id.tvBody)
        val tvChevron = itemView.findViewById<TextView>(R.id.tvChevron)
        val header = itemView.findViewById<View>(R.id.headerRow)

        tvTitle.text = title
        tvBody.text = body
        tvBody.visibility = View.GONE

        header.setOnClickListener {
            val isExpanded = tvBody.visibility == View.VISIBLE
            if (isExpanded) {
                collapse(tvBody)
                tvChevron.text = "›"
                tvChevron.rotation = 0f
            } else {
                expand(tvBody)
                tvChevron.text = "›"
                tvChevron.animate().rotation(90f).setDuration(200).start()
            }
        }

        container.addView(itemView)
    }

    private fun expand(view: View) {
        view.visibility = View.VISIBLE
        view.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        val targetHeight = view.measuredHeight
        view.layoutParams.height = 0

        val animator = ValueAnimator.ofInt(0, targetHeight)
        animator.duration = 220
        animator.addUpdateListener {
            view.layoutParams.height = it.animatedValue as Int
            view.requestLayout()
        }
        animator.start()
    }

    private fun collapse(view: View) {
        val initialHeight = view.measuredHeight
        val animator = ValueAnimator.ofInt(initialHeight, 0)
        animator.duration = 180
        animator.addUpdateListener {
            view.layoutParams.height = it.animatedValue as Int
            view.requestLayout()
            if (it.animatedValue as Int == 0) view.visibility = View.GONE
        }
        animator.start()
    }
}