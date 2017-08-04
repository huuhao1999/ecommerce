package products.api

import grails.rest.RestfulController

/**
 * Stock Controller
 *
 * Manages all operations that happens with Stock Entries
 */

class StockController extends RestfulController {
    static responseFormats = ['json'], allowedMethods = [index: "GET", save: "POST"]

    /**
     * Instantiates the Restful Controller
     *
     * @see Product
     */
    StockController() {
        super(Stock)
    }

    /**
     * Index Method
     *
     * Used to List Products or Search for Products
     *
     * @return a List of Products matching the search criteria or all Products if no criteria used
     */
    def index() {
        respond Stock.createCriteria().list(params) {
            if (params.name) {
                or {
                    eq('productId', Product.where {
                        name:
                        params.name
                    }.first().id)
                }
            }
            if (params.productId) {
                or { eq('id', params.productId) }
            }
        }
    }

    /**
     * Save Method
     *
     * Used to store a new Stock Entry related to a Product
     *
     * @return A success message if everything occurs correctly, an error message
     *  if an Invalid Input is given.
     */
    def save() {
        // Verifies if the Product Id parameter exists on the Body
        if (!request.JSON.productId) {
            response.status = 400

            respond(mesage: 'Invalid Identifier or no Identifier supplied')

            return
        }

        def product = Product.get(request.JSON.productId as Serializable)

        // Verifies if the Product Exists
        if (!product) {
            response.status = 404

            respond(message: 'Product not Found. Ensure that the Identifier it\'s correct.')

            return
        }

        def stock = Stock.create()

        bindData stock, request.JSON

        // We check if the Stock has a valid Mapping
        if (!stock.validate()) {
            response.status = 405

            respond(message: 'Invalid Input. Check your jSON.')

            return
        }

        def sum = Stock.findAllWhere(productId: product.id).amount.sum() + stock.amount

        // The sum of the amount cannot be less than zero. It's impossible have negative stock in someplace
        if (sum < 0) {
            response.status = 405

            respond(message: 'Invalid Input. Final Amount cannot be less than zero.')

            return
        }

        stock.save flush: true

        product.properties.stock = sum

        product.save flush: true

        respond(message: 'Stock Entry registered with Success', id: stock.id)
    }
}