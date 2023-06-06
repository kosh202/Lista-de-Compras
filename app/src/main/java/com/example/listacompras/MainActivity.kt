package com.example.listacompras

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.example.listacompras.databinding.ActivityMainBinding
import com.example.listacompras.databinding.ItemBinding
import com.firebase.ui.auth.AuthUI
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class MainActivity : AppCompatActivity() {

    lateinit var binding: ActivityMainBinding
    lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)

        setContentView(binding.root)

        tratarLogin()

        binding.fab.setOnClickListener{
            novoItem()
        }

    }

    fun tratarLogin(){
        if (FirebaseAuth.getInstance().currentUser == null)//ve se está logado
        {
            val providers = arrayListOf(AuthUI.IdpConfig.EmailBuilder().build())//somente de email que temos somente ele no firebase
            val intent = AuthUI.getInstance().createSignInIntentBuilder().setAvailableProviders(providers).build()

            startActivityForResult(intent, 1)
        }
        else{
            configurarBase()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 1 && resultCode == RESULT_OK) {//autrnticado
            Toast.makeText(this, "Autenticado", Toast.LENGTH_LONG).show()//temp 1 ou 3
            configurarBase()


        }

        else{
            finishAffinity()
        }
    }


    fun configurarBase(){//configurar firebase
        FirebaseAuth.getInstance().currentUser?.let {
            database = FirebaseDatabase.getInstance().reference.child(it.uid)

            val valueEventListener = object: ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    tratarDadosProdutos(snapshot)
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.w("MainActivity", "configurarBase", error.toException())
                    Toast.makeText(this@MainActivity, "Erro de conexão", Toast.LENGTH_LONG).show()
                }

            }
            database.child("produtos").addValueEventListener(valueEventListener)
        }
    }

    fun novoItem(){
        val editText = EditText(this)
        editText.hint = "nome do Item"

        AlertDialog.Builder(this)
            .setTitle("Novo Item")
            .setView(editText)
            .setPositiveButton("Inserir"){dialog, button->
                val produto = Produto(nome = editText.text.toString())
                val novoNo = database.child("produtos")//definido o caminho
                    .push()//cria novo no

                produto.id = novoNo.key
                novoNo.setValue(produto)

            }.create()
            .show()
    }

    fun tratarDadosProdutos(dataSnapshot: DataSnapshot){
        val listaProdutos = arrayListOf<Produto>()

        dataSnapshot.children.forEach{
            val produto = it.getValue(Produto::class.java)

            /*
            if (produto != null){
                listaProdutos.add(produto)

            }
            * */
            produto?.let {
                listaProdutos.add(it)

            }
        }
        atuakizarTela(listaProdutos)


    }

    fun atuakizarTela(lista: List<Produto>){

        binding.conteiner.removeAllViews()

        lista.forEach {
            //infla o elemento que representa 1 item
            val item = ItemBinding.inflate(layoutInflater)
            //configura os atributos no elemento
            item.nome.text = it.nome
            item.comprado.isChecked = it.comprado

            item.excluir.setOnClickListener{ view->
                it.id?.let{
                    val no = database.child("produtos").child(it)
                    no.removeValue()
                }

            }

            item.comprado.setOnCheckedChangeListener{button, isChecked ->
                it.id?.let{
                    val no = database.child("produtos").child(it)
                    no.child("comprado").setValue(isChecked)

                }

            }


            //coloca o  elemento dentro do conteiner
            binding.conteiner.addView(item.root)
        }
    }
}