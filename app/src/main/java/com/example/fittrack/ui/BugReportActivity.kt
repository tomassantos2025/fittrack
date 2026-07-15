package com.example.fittrack.ui

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.fittrack.R
import com.example.fittrack.databinding.ActivityBugReportBinding
import com.example.fittrack.repository.BugReportRepository
import com.example.fittrack.utils.LocaleHelper
import com.example.fittrack.utils.SystemBars
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

/**
 * Ecrã de reporte de bugs/feedback. Permite ao utilizador enviar uma descrição do problema para a base de dados.
 *
 * Comentário de manutenção: esta classe deve manter a lógica separada por responsabilidade,
 * para ser mais fácil alterar a app sem quebrar outros ecrãs.
 */
class BugReportActivity:AppCompatActivity(){ private lateinit var b:ActivityBugReportBinding; private val repo=BugReportRepository(); private var image:Uri?=null
 private val picker=registerForActivityResult(ActivityResultContracts.GetContent()){uri-> image=uri; b.ivPreview.setImageURI(uri); b.ivPreview.visibility=if(uri==null)View.GONE else View.VISIBLE}
 override fun attachBaseContext(newBase: Context){super.attachBaseContext(LocaleHelper.onAttach(newBase))}
 override fun onCreate(state:Bundle?){super.onCreate(state);b=ActivityBugReportBinding.inflate(layoutInflater);setContentView(b.root);SystemBars.applyTopInset(b.toolbar);b.btnBack.setOnClickListener{finish()};b.btnAttach.setOnClickListener{picker.launch("image/*")};b.btnSubmit.setOnClickListener{submit()}}
 private fun submit(){val title=b.etTitle.text?.toString().orEmpty();val desc=b.etDescription.text?.toString().orEmpty();if(title.isBlank()||desc.isBlank()){show(getString(R.string.bug_report_required));return};b.progressBar.visibility=View.VISIBLE;b.btnSubmit.isEnabled=false;lifecycleScope.launch{repo.submit(title,desc,image).onSuccess{show(getString(R.string.bug_report_sent));finish()}.onFailure{show(it.localizedMessage?:getString(R.string.error_generic))};b.progressBar.visibility=View.GONE;b.btnSubmit.isEnabled=true}}
 private fun show(m:String)=Snackbar.make(b.root,m,Snackbar.LENGTH_LONG).show()
}
