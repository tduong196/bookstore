package com.example.testdkdn

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object CartManager {
    private const val PREF_NAME = "cart_pref"
    private const val CART_KEY = "cart_items"

    fun addToCart(context: Context, book: Book) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()

        val currentCart = getCart(context).toMutableList()
        currentCart.add(book)

        val json = Gson().toJson(currentCart)
        editor.putString(CART_KEY, json)
        editor.apply()
    }

    fun getCart(context: Context): List<Book> {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(CART_KEY, null) ?: return emptyList()
        val type = object : TypeToken<List<Book>>() {}.type
        return Gson().fromJson(json, type)
    }

    fun clearCart(context: Context) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(CART_KEY).apply()
    }
    // Thêm vào CartManager.kt
    fun removeFromCart(context: Context, book: Book) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()

        val currentCart = getCart(context).toMutableList()
        currentCart.removeAll { it.id == book.id }

        val json = Gson().toJson(currentCart)
        editor.putString(CART_KEY, json)
        editor.apply()
    }
    // Thêm vào CartManager.kt
    fun updateQuantity(context: Context, book: Book, newQuantity: Int) {
        val currentCart = getCart(context).toMutableList()
        val index = currentCart.indexOfFirst { it.id == book.id }
        if (index != -1) {
            currentCart[index] = book.copy(quantity = newQuantity)
            saveCart(context, currentCart)
        }
    }

    private fun saveCart(context: Context, items: List<Book>) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(CART_KEY, Gson().toJson(items)).apply()
    }
    fun updateCartItem(context: Context, updatedBook: Book) {
        val cart = getCart(context).toMutableList()
        val index = cart.indexOfFirst { it.title == updatedBook.title }
        if (index != -1) {
            cart[index] = updatedBook
            saveCart(context, cart)
        }
    }

}
