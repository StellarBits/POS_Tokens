package com.stellarbitsapps.androidpdv.ui.reprint

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.stellarbitsapps.androidpdv.R
import com.stellarbitsapps.androidpdv.application.AndroidPdvApplication
import com.stellarbitsapps.androidpdv.databinding.FragmentReprintBinding
import com.stellarbitsapps.androidpdv.ui.adapter.ReprintAdapter
import com.stellarbitsapps.androidpdv.ui.adapter.ReprintListener
import com.stellarbitsapps.androidpdv.ui.custom.dialog.ProgressHUD
import com.stellarbitsapps.androidpdv.util.PrintUtils
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat

class ReprintFragment : Fragment() {

    companion object {
        fun newInstance() = ReprintFragment()
    }

    private val viewModel: ReprintViewModel by activityViewModels {
        ReprintViewModelFactory(
            (requireActivity().application as AndroidPdvApplication).database.reprintReport(),
            (requireActivity().application as AndroidPdvApplication).database.reportDao()
        )
    }

    private val binding: FragmentReprintBinding by lazy {
        FragmentReprintBinding.inflate(layoutInflater)
    }

    private lateinit var progressHUD: ProgressHUD

    @SuppressLint("SimpleDateFormat")
    @RequiresApi(Build.VERSION_CODES.N_MR1)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding.btClose.setOnClickListener {
            findNavController().navigate(R.id.initialCashFragment)
        }

        viewModel.reprintReport.observe(viewLifecycleOwner) {

            progressHUD = ProgressHUD.show(
                context, "Imprimindo relatório",
                cancelable = false,
                spinnerGone = false
            )
            progressHUD.show()

            lifecycleScope.launch {
                Log.i("JAO", "FinalDate: ${it.report.finalDate}")
                PrintUtils.printReport(
                    report = it.report,
                    sangrias = it.sangria,
                    errors = it.error,
                    finalDate = SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(it.report.finalDate ?: 0),
                    it.report.finalCash,
                    progressHUD
                )
            }
        }

        initRecyclerView()

        val titleScrollView = binding.titleHorizontalScrollView
        val contentScrollView = binding.contentHorizontalScrollView

        titleScrollView.setOnScrollChangeListener { _, scrollX, _, _, _ ->
            contentScrollView.scrollTo(scrollX, 0)
        }

        contentScrollView.setOnScrollChangeListener { _, scrollX, _, _, _ ->
            titleScrollView.scrollTo(scrollX, 0)
        }

        return binding.root
    }

    private fun initRecyclerView() {
        val reprintAdapter = ReprintAdapter(ReprintListener(
            clickListener = { report ->
                viewModel.getReport(report.id)
            }
        ))

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = reprintAdapter
        }

        viewModel.getAllReports()
        viewModel.report.observe(viewLifecycleOwner) {
            reprintAdapter.addHeadersAndSubmitList(it)
        }
    }
}