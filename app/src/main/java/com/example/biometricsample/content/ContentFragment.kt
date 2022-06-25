package com.example.biometricsample.content

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatButton
import androidx.navigation.fragment.findNavController
import com.example.biometricsample.R

class ContentFragment : Fragment() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val fragmentView = inflater.inflate(R.layout.fragment_content, container, false)
        fragmentView.findViewById<AppCompatButton>(R.id.bt_logout).setOnClickListener {
            logout()
        }
        return fragmentView
    }

    private fun logout(){
        findNavController().navigate(R.id.action_contentFragment_to_authFragment)
    }

}