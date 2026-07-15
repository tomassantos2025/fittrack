package com.example.fittrack.ui

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.fittrack.BuildConfig
import com.example.fittrack.R
import com.example.fittrack.databinding.ActivityAboutBinding
import com.example.fittrack.utils.LocaleHelper

/**
 * Ecrã Sobre. Mostra a identificação do autor, informação da aplicação e cumpre o requisito de about da cadeira.
 *
 * Comentário de manutenção: esta classe deve manter a lógica separada por responsabilidade,
 * para ser mais fácil alterar a app sem quebrar outros ecrãs.
 */
class AboutActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAboutBinding

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAboutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.about_title)
        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.tvVersion.text = getString(R.string.about_version, BuildConfig.VERSION_NAME)
        binding.tvCourse.text = getString(R.string.about_course)
        binding.tvDev1Name.text = getString(R.string.about_developer_1)
    }
}